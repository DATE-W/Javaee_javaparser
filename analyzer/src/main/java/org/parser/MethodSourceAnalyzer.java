package org.parser;

import java.util.Scanner;

public class MethodSourceAnalyzer {
    private String methodName;
    private String packageName;
    private String className;
    private int depth;

    public static void main(String[] args) {
        // 创建 MethodCallAnalyzer 对象
        MethodSourceAnalyzer analyzer = new MethodSourceAnalyzer();

        // 处理用户的输入
        ProjectAnalyzer projectAnalyzer = new ProjectAnalyzer("main");
        projectAnalyzer.findAllUsedExpr();
    }
}

