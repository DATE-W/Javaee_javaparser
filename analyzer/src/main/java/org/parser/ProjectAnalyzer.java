package org.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class ProjectAnalyzer implements Analyzable<ClassInfo> {
    private final List<File> javaFiles;
    private String packageName;

    public ProjectAnalyzer(String packageName) {
        this.packageName = packageName;
        File directory = new File("src/main/java/" + packageName); // 指定包的路径
        this.javaFiles = getJavaFiles(directory); // 获取包及其子目录中的所有Java文件;
    }

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

    public List<ClassInfo> analyze() {
        List<ClassInfo> classInfos = new ArrayList<>();
        JavaParser javaParser = new JavaParser();

        for (File javaFile : javaFiles) {
            try {
                CompilationUnit cu = javaParser.parse(javaFile).getResult().orElse(null);
                if (cu != null) {
                    List<ClassOrInterfaceDeclaration> classDeclarations = cu.findAll(ClassOrInterfaceDeclaration.class);
                    for (ClassOrInterfaceDeclaration classDeclaration : classDeclarations) {
                        ClassInfo classInfo = new ClassInfo(cu); // 传递CompilationUnit到ClassInfo
                        classInfo.analyze(); // 调用ClassInfo的analyze方法
                        classInfos.add(classInfo);
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        return classInfos;
    }

    public void analyzeSpecificMethod(String methodName, String className, int depth) {
        List<ClassInfo> classInfos = analyze(); // 获取所有类的信息

        for (ClassInfo classInfo : classInfos) {
            if (classInfo.getClassName().equals(className)) {
                List<MethodInfo> methodInfos = classInfo.getMethods();
                for (MethodInfo methodInfo : methodInfos) {
                    methodInfo.analyze(methodInfos);
                }
                for (MethodInfo methodInfo : methodInfos) {
                    if (methodInfo.getMethodName().equals(methodName)) {
                        System.out.println("Input:");
                        System.out.println(methodInfo.getMethodName() + ", " + className + ", depth=" + depth);
                        System.out.println("========");
                        System.out.println("Output:");
                        System.out.println("It is invoked by the following:\n" + methodInfo.getInvokedBy(depth, 1, packageName));
                        System.out.println("It invokes the following:\n" + methodInfo.getInvokes(depth, 1, packageName));
                    }
                }
            }
        }
    }

}
