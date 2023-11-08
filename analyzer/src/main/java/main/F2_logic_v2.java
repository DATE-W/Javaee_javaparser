package main;

import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.*;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;


import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Optional;

public class F2_logic_v2 {

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


            cu.findAll(MethodCallExpr.class).forEach(methodCall -> {
                // 获取MethodCallExpr的作用域
                Optional<Expression> scope = methodCall.getScope();
                if (scope.isPresent() && scope.get() instanceof NameExpr) {
                    NameExpr nameExpr = (NameExpr) scope.get();
                    String name = nameExpr.getNameAsString();

                    // 初始化resolvedType
                    ResolvedType resolvedType = null;

                    // 找到最近的声明
                    Node currentNode = methodCall;
                    Optional<VariableDeclarator> variableDeclarator = Optional.empty();

                    while (!(currentNode instanceof CompilationUnit)) {
                        if (currentNode instanceof BlockStmt) {
                            BlockStmt blockStmt = (BlockStmt) currentNode;
                            variableDeclarator = blockStmt.getStatements().stream()
                                    .filter(Statement::isExpressionStmt)
                                    .map(Statement::asExpressionStmt)
                                    .map(ExpressionStmt::getExpression)
                                    .filter(Expression::isVariableDeclarationExpr)
                                    .map(Expression::asVariableDeclarationExpr)
                                    .flatMap(vde -> vde.getVariables().stream())
                                    .filter(vd -> vd.getNameAsString().equals(name))
                                    .findFirst();

                            if (variableDeclarator.isPresent()) {
                                break;
                            }
                        }
                        else if (currentNode instanceof SwitchStmt) {
                            SwitchStmt switchStmt = (SwitchStmt) currentNode;
                            for (SwitchEntry switchEntry : switchStmt.getEntries()) {
                                for (Statement entryStmt : switchEntry.getStatements()) {
                                    // Now check if there's a variable declaration within the switch entry
                                    if (entryStmt.isExpressionStmt() && entryStmt.asExpressionStmt().getExpression().isVariableDeclarationExpr()) {
                                        VariableDeclarationExpr varDeclExpr = entryStmt.asExpressionStmt().getExpression().asVariableDeclarationExpr();
                                        Optional<VariableDeclarator> optVarDecl = varDeclExpr.getVariables().stream()
                                                .filter(vd -> vd.getNameAsString().equals(name))
                                                .findFirst();

                                        if (optVarDecl.isPresent()) {
                                            variableDeclarator = optVarDecl;
                                            break; // Break from the switchEntry loop
                                        }
                                    }
                                }
                                if (variableDeclarator.isPresent()) {
                                    break; // Break from the switchStmt loop if we've found our variable
                                }
                            }
                            if (variableDeclarator.isPresent()) {
                                break; // Break from the switchStmt loop if we've found our variable
                            }
                        }
                        // Check if we've reached an ObjectCreationExpr and if it contains the scope
                        else if (currentNode instanceof ObjectCreationExpr) {
                            ObjectCreationExpr objectCreationExpr = (ObjectCreationExpr) currentNode;
                            boolean scopeIsInObjectCreation = objectCreationExpr.getArguments().stream()
                                    .anyMatch(arg -> arg.toString().equals(name));
                            if (scopeIsInObjectCreation) {
                                break; // We found the scope in an object creation
                            }
                        }
                        currentNode = currentNode.getParentNode().orElse(null);
                    }

                    // Attempt to resolve the type if we've found the declaration
                    if (variableDeclarator.isPresent()) {
                        VariableDeclarator varDecl = variableDeclarator.get();
                        try {
                            // Check the initializer of the variable to determine the actual type
                            if (varDecl.getInitializer().isPresent() && varDecl.getInitializer().get().isObjectCreationExpr()) {
                                // If the variable is initialized using 'new', resolve the type of the created object
                                ObjectCreationExpr creationExpr = varDecl.getInitializer().get().asObjectCreationExpr();
                                resolvedType = JavaParserFacade.get(typeSolver).convertToUsage(creationExpr.getType(), methodCall);
                            } else {
                                // Otherwise, use the declared type
                                resolvedType = JavaParserFacade.get(typeSolver).getType(nameExpr);
                            }
                            // Output the qualified name of the type to see where the 'speak' method comes from
                            System.out.println(methodCall + " calls speak method of " + resolvedType.asReferenceType().getQualifiedName());
                        } catch (UnsolvedSymbolException e) {
                            System.err.println("Unresolved type for method call: " + methodCall);
                        } catch (RuntimeException e) {
                            System.err.println("Error resolving type for method call: " + methodCall + " - " + e.getMessage());
                        }
                    }
                }
            });
        } catch (Exception e) {
            // 主方法中的异常处理，打印堆栈跟踪
            e.printStackTrace();
        }
    }


    public void resolvePolymophicInvoke(){
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


            cu.findAll(MethodCallExpr.class).forEach(methodCall -> {
                // 获取MethodCallExpr的作用域
                Optional<Expression> scope = methodCall.getScope();
                if (scope.isPresent() && scope.get() instanceof NameExpr) {
                    NameExpr nameExpr = (NameExpr) scope.get();
                    String name = nameExpr.getNameAsString();

                    // 初始化resolvedType
                    ResolvedType resolvedType = null;

                    // 找到最近的声明
                    Node currentNode = methodCall;
                    Optional<VariableDeclarator> variableDeclarator = Optional.empty();

                    while (!(currentNode instanceof CompilationUnit)) {
                        if (currentNode instanceof BlockStmt) {
                            BlockStmt blockStmt = (BlockStmt) currentNode;
                            variableDeclarator = blockStmt.getStatements().stream()
                                    .filter(Statement::isExpressionStmt)
                                    .map(Statement::asExpressionStmt)
                                    .map(ExpressionStmt::getExpression)
                                    .filter(Expression::isVariableDeclarationExpr)
                                    .map(Expression::asVariableDeclarationExpr)
                                    .flatMap(vde -> vde.getVariables().stream())
                                    .filter(vd -> vd.getNameAsString().equals(name))
                                    .findFirst();

                            if (variableDeclarator.isPresent()) {
                                break;
                            }
                        }
                        else if (currentNode instanceof SwitchStmt) {
                            SwitchStmt switchStmt = (SwitchStmt) currentNode;
                            for (SwitchEntry switchEntry : switchStmt.getEntries()) {
                                for (Statement entryStmt : switchEntry.getStatements()) {
                                    // Now check if there's a variable declaration within the switch entry
                                    if (entryStmt.isExpressionStmt() && entryStmt.asExpressionStmt().getExpression().isVariableDeclarationExpr()) {
                                        VariableDeclarationExpr varDeclExpr = entryStmt.asExpressionStmt().getExpression().asVariableDeclarationExpr();
                                        Optional<VariableDeclarator> optVarDecl = varDeclExpr.getVariables().stream()
                                                .filter(vd -> vd.getNameAsString().equals(name))
                                                .findFirst();

                                        if (optVarDecl.isPresent()) {
                                            variableDeclarator = optVarDecl;
                                            break; // Break from the switchEntry loop
                                        }
                                    }
                                }
                                if (variableDeclarator.isPresent()) {
                                    break; // Break from the switchStmt loop if we've found our variable
                                }
                            }
                            if (variableDeclarator.isPresent()) {
                                break; // Break from the switchStmt loop if we've found our variable
                            }
                        }
                        // Check if we've reached an ObjectCreationExpr and if it contains the scope
                        else if (currentNode instanceof ObjectCreationExpr) {
                            ObjectCreationExpr objectCreationExpr = (ObjectCreationExpr) currentNode;
                            boolean scopeIsInObjectCreation = objectCreationExpr.getArguments().stream()
                                    .anyMatch(arg -> arg.toString().equals(name));
                            if (scopeIsInObjectCreation) {
                                break; // We found the scope in an object creation
                            }
                        }
                        currentNode = currentNode.getParentNode().orElse(null);
                    }

                    // Attempt to resolve the type if we've found the declaration
                    if (variableDeclarator.isPresent()) {
                        VariableDeclarator varDecl = variableDeclarator.get();
                        try {
                            // Check the initializer of the variable to determine the actual type
                            if (varDecl.getInitializer().isPresent() && varDecl.getInitializer().get().isObjectCreationExpr()) {
                                // If the variable is initialized using 'new', resolve the type of the created object
                                ObjectCreationExpr creationExpr = varDecl.getInitializer().get().asObjectCreationExpr();
                                resolvedType = JavaParserFacade.get(typeSolver).convertToUsage(creationExpr.getType(), methodCall);
                            } else {
                                // Otherwise, use the declared type
                                resolvedType = JavaParserFacade.get(typeSolver).getType(nameExpr);
                            }
                            // Output the qualified name of the type to see where the 'speak' method comes from
                            System.out.println(methodCall + " calls speak method of " + resolvedType.asReferenceType().getQualifiedName());
                        } catch (UnsolvedSymbolException e) {
                            System.err.println("Unresolved type for method call: " + methodCall);
                        } catch (RuntimeException e) {
                            System.err.println("Error resolving type for method call: " + methodCall + " - " + e.getMessage());
                        }
                    }
                }
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
