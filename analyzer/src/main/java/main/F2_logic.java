package main;

import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.*;
import com.github.javaparser.symbolsolver.*;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.*;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;


import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Optional;

public class F2_logic {

    // 用于存储变量名到类型声明的映射
    private static HashMap<String, ResolvedReferenceTypeDeclaration> typeMap = new HashMap<>();

    public static void main(String[] args) {
        try {
            // 解析Main.java文件
            FileInputStream in = new FileInputStream("src/main/java/main/test3.java");
            CompilationUnit cu = StaticJavaParser.parse(in);

            // 配置类型解析器，它用于解析项目中的类型
            TypeSolver typeSolver = new CombinedTypeSolver(
                    new ReflectionTypeSolver(), // 解析JRE类
                    new JavaParserTypeSolver(new java.io.File("src/main/java"))); // 解析项目中的类

            // 用类型解析器创建一个JavaParserFacade实例，它提供解析的高级接口
            JavaParserFacade facade = JavaParserFacade.get(typeSolver);

            // 收集所有的对象创建表达式（例如 new SomeClass()），以确定变量实际引用的类型
            cu.findAll(ObjectCreationExpr.class).forEach(objCreation -> {
                // 获取对象创建表达式的类型
                ResolvedType resolvedType = facade.convertToUsage(objCreation.getType(), objCreation);
                // 如果类型是引用类型，获取其声明
                if (resolvedType.isReferenceType()) {
                    ResolvedReferenceTypeDeclaration typeDeclaration = resolvedType.asReferenceType().getTypeDeclaration().get();
                    // 尝试找到变量声明节点的祖先节点
                    objCreation.findAncestor(VariableDeclarator.class).ifPresent(varDecl -> {
                        String scopePath=getScopePath(cu, objCreation);
                        // 如果没有找到作用域标识符，使用"unknown"作为默认值
                        if (scopePath.length() == 0) {
                            scopePath="unknown";
                        }
                        // 创建一个包含作用域信息的键
                        String key = scopePath + "." + varDecl.getNameAsString();
                        // 将变量名和类型声明放入映射中
                        typeMap.put(key, typeDeclaration);
                    });
                }
            });

            // 解析所有的方法调用表达式
            cu.findAll(MethodCallExpr.class).forEach(methodCall -> {
                Optional<Expression> optScope = methodCall.getScope();
                optScope.ifPresent(scope -> {
                    if (scope instanceof NameExpr) {
                        String name = ((NameExpr) scope).getNameAsString();

                        // 获取包含方法调用的方法声明或类/接口声明的作用域标识符
                        StringBuilder scopeIdentifier = new StringBuilder();
                        Optional<MethodDeclaration> methodDecl = methodCall.findAncestor(MethodDeclaration.class);
                        if (methodDecl.isPresent()) {
                            MethodDeclaration decl = methodDecl.get();
                            ClassOrInterfaceDeclaration classDecl = decl.findAncestor(ClassOrInterfaceDeclaration.class).orElse(null);
                            if (classDecl != null) {
                                Optional<PackageDeclaration> pkgDecl = cu.getPackageDeclaration();
                                if (pkgDecl.isPresent()) {
                                    scopeIdentifier.append(pkgDecl.get().getNameAsString()).append(".");
                                }
                                scopeIdentifier.append(classDecl.getNameAsString()).append(".");
                            }
                            scopeIdentifier.append(decl.getNameAsString()).append(".");
                        } else {
                            // 如果没有方法祖先，我们可能在一个初始化块或字段声明中
                            methodCall.findAncestor(ClassOrInterfaceDeclaration.class).ifPresent(classOrInterfaceDecl -> {
                                String className = classOrInterfaceDecl.getNameAsString();
                                classOrInterfaceDecl.findAncestor(PackageDeclaration.class).ifPresent(pkgDecl -> {
                                    String packageName = pkgDecl.getNameAsString();
                                    scopeIdentifier.append(packageName).append(".");
                                });
                                scopeIdentifier.append(className).append(".");
                            });
                        }

                        // 使用作用域和变量名构建键
                        String key = scopeIdentifier + name;
                        // 从映射中获取变量对应的实际类型
                        ResolvedReferenceTypeDeclaration actualType = typeMap.get(key);
                        // 如果能找到实际类型
                        if (actualType != null) {
                            try {
                                // 解析方法调用的声明
                                ResolvedMethodDeclaration method = facade.solve(methodCall).getCorrespondingDeclaration();
                                // 在实际类型中查找对应的方法
                                Optional<ResolvedMethodDeclaration> actualMethod = actualType.getDeclaredMethods().stream()
                                        .filter(m -> m.getName().equals(method.getName()))
                                        .findFirst();

                                // 如果找到，打印方法调用和声明的详细信息
                                if (actualMethod.isPresent()) {
                                    System.out.println("Method call: " + methodCall);
                                    System.out.println("Declaration: " + actualMethod.get().getQualifiedSignature());
                                } else {
                                    // 如果未找到在实际类型中的声明，打印原始解析结果
                                    System.out.println("Method call: " + methodCall);
                                    System.out.println("Declaration: " + method.getQualifiedSignature());
                                }
                            } catch (Exception e) {
                                // 解析出现异常，打印错误信息
                                System.out.println("Resolution error: " + e.getMessage());
                            }
                        }
                    }
                });
            });
        } catch (Exception e) {
            // 主方法中的异常处理，打印堆栈跟踪
            e.printStackTrace();
        }
    }

    public static String getScopePath(CompilationUnit cu, ObjectCreationExpr objectCreation){
        // 递归构建完整的嵌套路径，包括方法和类名
        StringBuilder nestedPath = new StringBuilder();
        Node currentNode = objectCreation;

        while (currentNode != null) {
            if (currentNode instanceof MethodDeclaration) {
                MethodDeclaration method = (MethodDeclaration) currentNode;
                if (nestedPath.length() > 0) {
                    nestedPath.insert(0, ".");
                }
                nestedPath.insert(0, method.getNameAsString());
            } else if (currentNode instanceof ClassOrInterfaceDeclaration) {
                ClassOrInterfaceDeclaration classOrInterface = (ClassOrInterfaceDeclaration) currentNode;
                if (nestedPath.length() > 0) {
                    nestedPath.insert(0, ".");
                }
                nestedPath.insert(0, classOrInterface.getNameAsString());
            }
            currentNode = currentNode.getParentNode().orElse(null);
        }

        return cu.getPackageDeclaration()+"."+nestedPath.toString();
    }
}
