package org.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class ProjectAnalyzer implements Analyzable<ClassInfo> {
    // 存储项目中的Java文件列表
    private final List<File> javaFiles;
    // 项目的包名
    private String packageName;

    // 已配置的JavaParser实例
    public static JavaParser CONFIGURED_PARSER;

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
        List<ClassInfo> classInfos = analyze();
        List<MethodInfo> methodInfos = new ArrayList<>();

        for (ClassInfo classInfo : classInfos) {
            methodInfos.addAll(classInfo.getMethods());
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
}
