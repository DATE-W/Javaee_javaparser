package org.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.Queue;

public class ParsersAdapter {
    public static JavaParser CONFIGURED_PARSER; // 配置好的 JavaParser 对象
    String path; // 项目路径
    public ParsersAdapter(String path) {
        configure(path); // 配置 CONFIGURED_PARSER
        this.path = path; // 设置项目路径
    }
    public void configure(String path) {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        combinedTypeSolver.add(new JavaParserTypeSolver(new File(path)));
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        ParserConfiguration parserConfig = new ParserConfiguration();
        parserConfig.setSymbolResolver(symbolSolver);
        CONFIGURED_PARSER = new JavaParser(parserConfig);
    }

    // 分析项目目录下的所有 java 文件
    public void analyzeAllFiles(Invocations invocations) {
        File directory = new File(path + "/main"); // 项目目录
        Queue<File> queue = new LinkedList<>(); // 基于链表的队列
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

    public void parseFile(File file, Invocations invocations) {
        JavaParser javaParser = CONFIGURED_PARSER; // 使用配置好的 parser
        try {
            CompilationUnit cu = javaParser.parse(file).getResult().orElse(null);
            if (cu == null) {
                return;
            }
            String packageName = cu.getPackageDeclaration().get().getNameAsString(); // 获取包名
            for (ClassOrInterfaceDeclaration classDeclaration: cu.findAll(ClassOrInterfaceDeclaration.class)) {
                parseClass(packageName, classDeclaration, invocations);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    public void parseClass(String packageName, ClassOrInterfaceDeclaration declaration, Invocations invocations) {
        String className = declaration.getNameAsString(); // 获取类名
        for (MethodDeclaration methodDeclaration : declaration.getMethods()) {
            parseMethod(packageName, className, methodDeclaration, invocations);
        }
    }

    public void parseMethod(String packageName, String className, MethodDeclaration declaration, Invocations invocations) {
        invocations.addNode(packageName, className, declaration); // 将方法结点加入图中
        String caller = String.format("%s.%s.%s", packageName, className, declaration.getNameAsString()); // 方法结点的标识符
        for (MethodCallExpr methodCall : declaration.findAll(MethodCallExpr.class)) {
            try {
                String[] calleeInfo = methodCall.resolve().getQualifiedSignature().split("\\."); // 被调用方法信息
                String calleeName = methodCall.getNameAsString();
                String calleeClass = null;
                String calleePackage = null;
                for (int i = calleeInfo.length - 1; i >= 2; i--) {
                    if (calleeInfo[i].contains(calleeName)) {
                        calleeClass = calleeInfo[i - 1];
                        calleePackage = calleeInfo[i - 2];
                        break;
                    }
                }
                if (calleePackage == null || calleeClass == null) {
                    continue;
                }
                String callee = String.format("%s.%s.%s", calleePackage, calleeClass, calleeName); // 被调用方法标识符
                invocations.addNode(calleePackage, calleeClass, calleeName); // 被调用方法加入图中
                invocations.addEdge(caller, callee); // 建立关系
            } catch (Exception e) {
                System.out.println(e);
                System.out.println("无法解析方法调用: " + methodCall);
            }
        }
    }

}
