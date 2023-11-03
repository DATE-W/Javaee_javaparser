package animal;

import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.*;
import com.github.javaparser.symbolsolver.*;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.*;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Optional;

public class MethodCallResolver {

    // 用于存储变量名到类型声明的映射
    private static HashMap<String, ResolvedReferenceTypeDeclaration> typeMap = new HashMap<>();

    public static void main(String[] args) {
        try {
            // 解析Main.java文件
            FileInputStream in = new FileInputStream("src/main/java/animal/Main.java");
            CompilationUnit cu = StaticJavaParser.parse(in);

            // 配置类型解析器，它用于解析项目中的类型
            TypeSolver typeSolver = new CombinedTypeSolver(
                    new ReflectionTypeSolver(), // 解析JRE类
                    new JavaParserTypeSolver(new java.io.File("src/main/java"))); // 解析项目中的类

            // 设置解析器配置，包括符号解析器
            ParserConfiguration parserConfiguration = new ParserConfiguration()
                    .setSymbolResolver(new JavaSymbolSolver(typeSolver));
            JavaParser javaParser = new JavaParser(parserConfiguration);

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
                        // 将变量名和类型声明放入映射中
                        typeMap.put(varDecl.getNameAsString(), typeDeclaration);
                    });
                }
            });

            // 解析所有的方法调用表达式
            cu.findAll(MethodCallExpr.class).forEach(methodCall -> {
                // 获取方法调用的作用域，如变量名或者this
                Optional<Expression> optScope = methodCall.getScope();
                optScope.ifPresent(scope -> {
                    // 如果作用域是变量名
                    if (scope instanceof NameExpr) {
                        // 获取变量名
                        String name = ((NameExpr) scope).getNameAsString();
                        // 从映射中获取变量对应的实际类型
                        ResolvedReferenceTypeDeclaration actualType = typeMap.get(name);
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
}
