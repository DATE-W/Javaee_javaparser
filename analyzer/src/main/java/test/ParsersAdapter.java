package test;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class ParsersAdapter {
    public static JavaParser CONFIGURED_PARSER; // 配置好的 JavaParser 对象
    String path; // 项目路径

    // 构造函数
    public ParsersAdapter(String path) {
        try {
            configure(path); // 配置 CONFIGURED_PARSER
            this.path = path; // 设置项目路径
        } catch (IOException e) {
            // 处理异常，例如记录错误日志或采取其他措施
            e.printStackTrace();
        }
    }


    // 配置 JavaParser
    public void configure(String path) throws IOException {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        combinedTypeSolver.add(new JavaParserTypeSolver(new File(path)));

        // 递归查找指定目录下的所有 .jar 文件并添加到类型解析器中
        File m2Repository = new File("/Users/teal/.m2/repository/com/github/javaparser");
        List<File> jarFiles = findJarFiles(m2Repository);
        for (File jarFile : jarFiles) {
            combinedTypeSolver.add(JarTypeSolver.getJarTypeSolver(jarFile.getAbsolutePath()));
        }

        // 添加Java标准库的类型解析器
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            File modulesFolder = new File(javaHome, "lib/modules"); // 对于Java 9及更高版本
            if (modulesFolder.exists() && modulesFolder.isDirectory()) {
                for (File moduleFile : Objects.requireNonNull(modulesFolder.listFiles())) {
                    if (moduleFile.isFile() && moduleFile.getName().endsWith(".jar")) {
                        combinedTypeSolver.add(JarTypeSolver.getJarTypeSolver(moduleFile.getAbsolutePath()));
                    }
                }
            }
        }


        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        ParserConfiguration parserConfig = new ParserConfiguration();
        parserConfig.setSymbolResolver(symbolSolver);
        CONFIGURED_PARSER = new JavaParser(parserConfig);
    }


    private static List<File> findJarFiles(File directory) {
        List<File> jarFiles = new ArrayList<>();
        if (directory != null && directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        // 如果是目录，递归查找
                        jarFiles.addAll(findJarFiles(file));
                    } else if (file.isFile() && file.getName().endsWith(".jar")) {
                        // 如果是 .jar 文件，添加到列表中
                        jarFiles.add(file);
                    }
                }
            }
        }
        return jarFiles;
    }

    // 分析项目目录下的所有 java 文件
    public void analyzeAllFiles(Invocations invocations) {
        File directory = new File(path + "/org/parser"); // 项目目录
        Queue<File> queue = new LinkedList<>(); // 基于链表的队列

        // 将所有方法加入图中
        queue.add(directory); // 根结点入队
        while (!queue.isEmpty()) {
            File target = queue.remove(); // 取队头
            if (target.isFile()) { // 文件为叶结点
                if (target.getName().endsWith(".java")) {
                    addAllMethodsFromFile(target, invocations); // 分析文件
                }
                continue;
            }
            for (File son : target.listFiles()) {
                queue.add(son); // 子结点入队
            }
        }

        // 将所有调用关系加入图中
        queue.add(directory); // 根结点入队
        while (!queue.isEmpty()) {
            File target = queue.remove(); // 取队头
            if (target.isFile()) { // 文件为叶结点
                if (target.getName().endsWith(".java")) {
                    parseFile(target, invocations); // 分析文件
                }
                continue;
            }
            for (File son : target.listFiles()) {
                queue.add(son); // 子结点入队
            }
        }
    }

    // 将文件中的所有方法加入图中
    public void addAllMethodsFromFile(File file, Invocations invocations) {
        JavaParser javaParser = CONFIGURED_PARSER; // 使用配置好的 parser
        try {
            CompilationUnit cu = javaParser.parse(file).getResult().orElse(null);
            if (cu == null) {
                return;
            }
            String packageName = cu.getPackageDeclaration().get().getNameAsString(); // 获取包名
            for (ClassOrInterfaceDeclaration classDeclaration : cu.findAll(ClassOrInterfaceDeclaration.class)) { // 遍历所有类
                String className = classDeclaration.getNameAsString(); // 获取类名
                for (MethodDeclaration methodDeclaration : classDeclaration.getMethods()) { // 遍历所有方法
                    invocations.addNode(packageName, className, methodDeclaration); // 添加方法结点
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    // 分析文件中的所有调用语句
    public void parseFile(File file, Invocations invocations) {
        JavaParser javaParser = CONFIGURED_PARSER; // 使用配置好的 parser
        try {
            CompilationUnit cu = javaParser.parse(file).getResult().orElse(null);
            if (cu == null) {
                return;
            }
            String packageName = cu.getPackageDeclaration().get().getNameAsString(); // 获取包名
            for (ClassOrInterfaceDeclaration classDeclaration : cu.findAll(ClassOrInterfaceDeclaration.class)) { // 遍历类
                String className = classDeclaration.getNameAsString(); // 获取类名
                for (MethodDeclaration methodDeclaration : classDeclaration.getMethods()) { // 遍历方法
                    Methods caller = invocations.findMethod(Methods.constructIdentifier(packageName, className, methodDeclaration.getNameAsString(), methodDeclaration.getParameters()));
                    for (MethodCallExpr methodCall : methodDeclaration.findAll(MethodCallExpr.class)) {
                        try {

                            System.out.println(methodCall.getNameAsString());
                            String[] calleeInfo = methodCall.resolve().getQualifiedSignature().split("[()]"); // 被调用方法信息
                            String calleeName = methodCall.getNameAsString();

                            if (calleeInfo.length != 2) {
                                continue;
                            }
                            String[] calleeBody = calleeInfo[0].split("\\.");
                            String[] calleeParamsType = calleeInfo[1].split(", ");

                            // 获取被调用者的类和包

                            /* 注意：这里在完成 F2 之后需要继续完善 */
                            String calleeClass = null;
                            String calleePackage = null;
                            for (int i = calleeBody.length - 1; i >= 2; i--) {
                                if (calleeBody[i].contains(calleeName)) {
                                    calleeClass = calleeBody[i - 1];
                                    calleePackage = calleeBody[i - 2];
                                    break;
                                }
                            }
                            if (calleePackage == null || calleeClass == null) {
                                continue;
                            }

                            // 构造参数类型列表
                            ArrayList<String> calleeParamsList = new ArrayList<>();
                            for (String paramType : calleeParamsType) {
                                if (paramType.contains(".")) {
                                    calleeParamsList.add(paramType.substring(paramType.lastIndexOf('.') + 1));
                                } else {
                                    calleeParamsList.add(paramType);
                                }
                            }
                            invocations.addNode(calleePackage, calleeClass, calleeName, calleeParamsList); // 若不存在则加入图中
                            Methods callee = invocations.findMethod(Methods.constructIdentifier(calleePackage, calleeClass, calleeName, calleeParamsList)); // 找到被调用函数
                            invocations.addEdge(caller, callee); // 建立关系
                        } catch (Exception e) {
                            System.out.println(e);
                            System.out.println("无法解析方法调用: " + methodCall);
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
