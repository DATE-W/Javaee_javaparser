package org.parser;

import java.util.Scanner;

public class MethodCallAnalyzer {
    private String methodName;
    private String packageName;
    private String className;
    private int depth;

    public static void main(String[] args) {
        // 创建Scanner对象，用于获取用户的输入
        Scanner scanner = new Scanner(System.in);

        // 提示用户输入信息
        System.out.println("请输入方法信息（格式如：introduction, main.Test, depth=2）：");

        // 获取用户输入的字符串
        String userInput = scanner.nextLine();

        userInput = userInput.replaceAll("\\s", "");

        // 关闭Scanner对象
        scanner.close();

        // 创建MethodCallAnalyzer对象
        MethodCallAnalyzer analyzer = new MethodCallAnalyzer();

        // 处理用户的输入
        analyzer.analyzeMethodCall(userInput);
    }

    public void analyzeMethodCall(String userInput) {
        // 分割用户输入的字符串，获取各个部分的信息
        String[] parts = userInput.split(",");

        if (parts.length == 3) {
            methodName = parts[0].trim();
            String fullClassName = parts[1].trim();
            String depthInfo = parts[2].trim();

            String[] packageClass = fullClassName.split("\\.");

            // 从depthInfo字符串中提取数字
            String[] depthParts = depthInfo.split("=");
            if (packageClass.length == 2 && depthParts.length == 2 && depthParts[0].trim().equalsIgnoreCase("depth")) {
                packageName = packageClass[0];
                className = packageClass[1];
                depth = Integer.parseInt(depthParts[1].trim());
            } else {
                System.out.println("深度信息格式错误，请按照指定格式输入（例如：introduction, main.Test, depth=2）");
            }
        } else {
            System.out.println("输入格式错误，请按照指定格式输入（例如：introduction, main.Test, depth=2）");
        }

        ProjectAnalyzer projectAnalyzer = new ProjectAnalyzer(packageName);
        projectAnalyzer.analyzeSpecificMethod(methodName, className, depth);
    }
}
