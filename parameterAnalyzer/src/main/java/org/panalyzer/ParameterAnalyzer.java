package org.panalyzer;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class ParameterAnalyzer {
    private List<MethodCallInfo> methodCallInfos;

    public ParameterAnalyzer() {
        this.methodCallInfos = new ArrayList<>();
    }

    public void analyzeProject(String projectPath) {
        // Load Java files
        List<File> javaFiles = loadJavaFiles(new File(projectPath));

        // Analyze methods in each Java file
        for (File file : javaFiles) {
            try {
                CompilationUnit cu = StaticJavaParser.parse(file);
                List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);
                for (MethodDeclaration method : methods) {
                    analyzeMethod(method);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private List<File> loadJavaFiles(File directory) {
        List<File> javaFiles = new ArrayList<>();
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    javaFiles.addAll(loadJavaFiles(file));
                } else if (file.getName().endsWith(".java")) {
                    javaFiles.add(file);
                }
            }
        }

        return javaFiles;
    }


    private void analyzeMethod(MethodDeclaration method) {
        method.findAll(MethodCallExpr.class).forEach(this::analyzeMethodCall); // Call the analyzeMethodCall method
    }



    private void analyzeMethodCall(MethodCallExpr methodCall) {
        MethodCallInfo methodCallInfo = new MethodCallInfo(methodCall.getNameAsString());
        methodCallInfos.add(methodCallInfo);

        for (int i = 0; i < methodCall.getArguments().size(); i++) {
            ParameterInfo paramInfo = new ParameterInfo(i, "Type", methodCall.getArgument(i).toString());
            methodCallInfo.addParameter(paramInfo);

            // Assuming BasicParameterUsage is properly initialized
            ParameterUsage usage = new BasicParameterUsage("Value", true, "Location");
            paramInfo.addUsage(usage);
        }
    }

    public void displayResults() {
        // Display the analysis results
        methodCallInfos.forEach(methodCallInfo -> {
            System.out.println("Method: " + methodCallInfo.getMethodName());
            for (ParameterInfo paramInfo : methodCallInfo.getParameters()) {
                System.out.println("  Parameter: " + paramInfo.getName());
                for (ParameterUsage usage : paramInfo.getUsages()) {
                    System.out.println("    Usage: " + usage.getValue() + " at " + usage.getLocation());
                }
            }
        });
    }
}
