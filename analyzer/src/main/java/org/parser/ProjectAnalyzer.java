package org.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.ast.body.Parameter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;


public class ProjectAnalyzer implements Analyzable<ClassInfo> {
    // 存储项目中的Java文件列表
    private final List<File> javaFiles;
    // 项目的包名
    private String packageName;

    // 已配置的JavaParser实例
    public static JavaParser CONFIGURED_PARSER;

    public CallGraph graph;

    // 构造函数，初始化分析器并配置JavaParser
    public ProjectAnalyzer(String packageName) {
        this.packageName = packageName;
        // 获取包目录下的所有Java文件
        File directory = new File("src/main/java/" + packageName);
        this.javaFiles = getJavaFiles(directory);

        // 配置符号解析器，用于解析Java代码中的符号
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        combinedTypeSolver.add(new JavaParserTypeSolver(new File("src/main/java/")));
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);

        // 创建一个新的ParserConfiguration对象
        ParserConfiguration parserConfig = new ParserConfiguration();
        // 配置符号解析器
        parserConfig.setSymbolResolver(symbolSolver);

        // 使用新的配置创建JavaParser实例
        CONFIGURED_PARSER = new JavaParser(parserConfig);
    }

    // 递归获取目录下的所有Java文件
    public static List<File> getJavaFiles(File directory) {
        List<File> javaFiles = new ArrayList<>();
        File[] files = directory.listFiles(); // 获取目录中的所有文件和子目录

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // 如果是一个目录，则递归遍历
                    javaFiles.addAll(getJavaFiles(file));
                } else if (file.getName().endsWith(".java")) {
                    // 如果是一个Java文件，则添加到列表中
                    javaFiles.add(file);
                }
            }
        }

        return javaFiles;
    }

    // 分析项目中的类，并返回类信息列表
    public List<ClassInfo> analyze() {
        List<ClassInfo> classInfos = new ArrayList<>();
        JavaParser javaParser = CONFIGURED_PARSER;

        for (File javaFile : javaFiles) {
            try {
                // 解析Java文件
                CompilationUnit cu = javaParser.parse(javaFile).getResult().orElse(null);
                if (cu != null) {
                    // 获取文件中的所有类声明
                    List<ClassOrInterfaceDeclaration> classDeclarations = cu.findAll(ClassOrInterfaceDeclaration.class);
                    for (ClassOrInterfaceDeclaration classDeclaration : classDeclarations) {
                        // 创建类信息对象并分析
                        ClassInfo classInfo = new ClassInfo(cu);
                        classInfo.analyze();
                        classInfos.add(classInfo);
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        return classInfos;
    }

    // 分析指定类中的特定方法，并显示其调用的方法和被哪些方法调用
    public void analyzeSpecificMethod(String methodName, String className, int depth) {
        List<ClassInfo> classInfos = analyze();     // 调用 analyze 获取所有类的信息
        List<MethodInfo> methodInfos = new ArrayList<>();

        for (ClassInfo classInfo : classInfos) {
            methodInfos.addAll(classInfo.getMethods());     // 把 classInfos 里的信息加入 methodInfos，把一个列表中的元素加到另一个列表中
        }
        for (MethodInfo methodInfo : methodInfos) {
            methodInfo.analyze(methodInfos);
        }

        for (MethodInfo methodInfo : methodInfos) {
            if (methodInfo.getMethodName().equals(methodName) && methodInfo.getClassName().equals(className)) {
                System.out.println("Input:");
                System.out.println(methodInfo.getMethodName() + ", " + className + ", depth=" + depth);
                System.out.println("========");
                System.out.println("Output:");
                String tempInvoked = methodInfo.getInvokedBy(depth, 1, packageName);
                String tempInvokes = methodInfo.getInvokes(depth, 1, packageName);
                if (!tempInvoked.equals("")) {
                    System.out.println("It is invoked by the following:\n" + tempInvoked);
                } else {
                    System.out.println("It is invoked by the following:\n[NONE]");
                }
                if (!tempInvokes.equals("")) {
                    System.out.println("It invokes the following:\n" + tempInvokes);
                } else {
                    System.out.println("It invokes the following:\n[NONE]");
                }
            }
        }
    }
    // 查看被调用的方法

    public void methodTracingAnalyze() {
        List<ClassInfo> classInfos = analyze();     // 调用 analyze 获取所有类的信息

        for (ClassInfo classInfo : classInfos) {
            List<MethodInfo> methodInfos = classInfo.getMethods();
            for (MethodInfo methodInfo : methodInfos) {
                methodInfo.analyze(methodInfos);
            }
//            System.out.println("分析的类" + classInfo.getClassName());
            for (MethodInfo methodInfo : methodInfos) {
//                System.out.println("分析的函数" + methodInfo.getMethodName());
                methodInfo.getInvokedParameters();
//                System.out.println("\n");
            }
        }
    }


    // 这里后面要改成ClassInfo版本的
    public void findAllUsedExpr() {
        JavaParser javaParser = CONFIGURED_PARSER;

        for (File javaFile : javaFiles) {
            try {
                // Parse the Java file.
                CompilationUnit cu = javaParser.parse(javaFile).getResult().orElse(null);
                if (cu != null) {
                    // 对所有的类遍历做处理
                    List<ClassOrInterfaceDeclaration> classDeclarations = cu.findAll(ClassOrInterfaceDeclaration.class);
                    for (ClassOrInterfaceDeclaration classDeclaration : classDeclarations) {

                        // 先去找赋值的情况
                        List<AssignExpr> assignExprs = classDeclaration.findAll(AssignExpr.class);
                        for (AssignExpr assignExpr : assignExprs) {
                            // 获取赋值前面的内容
                            Expression leftExpr = assignExpr.getTarget();
                            // 获取赋值后面的内容
                            Expression rightExpr = assignExpr.getValue();

                            // 创建 CallNode，并将赋值前面的内容添加到 CallNode
                            CallNode callNode = new CallNode(rightExpr.toString(), classDeclaration.getNameAsString(), -1);

                            // 如果赋值后面的内容不为空，将其作为 nextNode 添加到 CallNode
                            if (rightExpr != null) {
                                CallNode nextNode = new CallNode(leftExpr.toString(), classDeclaration.getNameAsString(), -1);
                                callNode.addNextNode(nextNode);
                            }

                            // 将 CallNode 添加到 CallGraph
                            graph.addNode(callNode);

                            // 打印赋值情况
                            System.out.println("Found an assignment expression in class " +
                                    classDeclaration.getNameAsString() + ": " + assignExpr);
                        }


                        // 再去找声明的情况
                        List<VariableDeclarationExpr> variableDeclarations = classDeclaration.findAll(VariableDeclarationExpr.class);
                        for (VariableDeclarationExpr variableDeclaration : variableDeclarations) {
                            // 检查是否是类声明
                            List<VariableDeclarator> variables = variableDeclaration.getVariables();
                            for (VariableDeclarator variable : variables) {
                                String variableName = variable.getNameAsString(); // 获取变量的名称
                                Expression expr = variable.getInitializer().orElse(null); // 获取初始化表达式
                                if (expr != null) {
                                    // 创建 CallNode，并将初始化表达式添加到 CallNode
                                    CallNode callNode = new CallNode(expr.toString(), classDeclaration.getNameAsString(), -1);

                                    // 创建 CallNode，并将变量名称添加到 CallNode
                                    CallNode nextNode = new CallNode(variableName, classDeclaration.getNameAsString(), -1);

                                    // 将 nextNode 添加到 callNode 的 nextNodes 列表中
                                    callNode.addNextNode(nextNode);

                                    // 将 callNode 添加到 CallGraph
                                    graph.addNode(callNode);
                                }
                            }
                        }


                        // 再去找函数调用的情况
                        List<MethodCallExpr> methodCalls = classDeclaration.findAll(MethodCallExpr.class);
                        for (MethodCallExpr methodCall : methodCalls) {
                            String methodName = methodCall.getNameAsString(); // 获取方法名
                            NodeList<Expression> arguments = methodCall.getArguments(); // 获取参数列表
                            // 打印找到的函数调用和参数信息
                            System.out.println("Found a method call in class " + classDeclaration.getNameAsString() +
                                    ": Method Name: " + methodName + ", Arguments: " + arguments);
                        }
                        // 然后对于函数调用，要去进入它的内部，函数是作为一个中介，也就是nextNode指向函数
                    }

                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }


}
