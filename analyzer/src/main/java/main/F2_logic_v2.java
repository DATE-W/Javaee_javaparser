package main;

import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.*;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;


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
                // 获取'speak'方法调用的作用域（'b'）
                Optional<Expression> optScope = methodCall.getScope();
                if (optScope.isPresent() && optScope.get() instanceof NameExpr) {
                    String scopeName = ((NameExpr) optScope.get()).getNameAsString();
                    // 在当前作用域中查找变量'b'的声明
                    Node currentNode = methodCall;
                    while (currentNode != null) {
                        if (currentNode instanceof BlockStmt) {
                            BlockStmt blockStmt = (BlockStmt) currentNode;
                            // 在当前块中查找变量'b'的声明
                            blockStmt.getStatements().stream()
                                    .filter(stmt -> stmt.isExpressionStmt() && stmt.asExpressionStmt().getExpression().isVariableDeclarationExpr())
                                    .map(stmt -> stmt.asExpressionStmt().getExpression().asVariableDeclarationExpr())
                                    .flatMap(varDeclExpr -> varDeclExpr.getVariables().stream())
                                    .filter(varDecl -> varDecl.getNameAsString().equals(scopeName))
                                    .forEach(varDecl -> {
                                        // 如果找到了变量'b'的声明，检查它的初始化类型
                                        if (varDecl.getInitializer().isPresent() && varDecl.getInitializer().get().isObjectCreationExpr()) {
                                            ObjectCreationExpr creationExpr = varDecl.getInitializer().get().asObjectCreationExpr();
                                            String typeName = creationExpr.getType().getNameAsString();
                                            System.out.println("Variable 'b' is initialized as a new instance of: " + typeName);
                                        }
                                    });
                        }
                        currentNode = currentNode.getParentNode().orElse(null);
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
