package org.parser;

import java.util.Scanner;

class MethodCallAnalyzer {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in); // 创建 scanner 用于读取系统输入
        System.out.println("请输入方法信息（格式如：introduction, main.Test, depth=2）："); // 输出提示信息
        String userInput = scanner.nextLine(); // 从 scanner 中获取用户输入字符串
        userInput = userInput.replaceAll("\\s", ""); // 去除空白符
        scanner.close(); // 关闭 scanner
        MethodCallAnalyzer analyzer = new MethodCallAnalyzer(); // 创建 analyzer
        analyzer.analyzeMethodCall(userInput); // 分析用户输入
    }

    public void analyzeMethodCall(String userInput) {
        // 用 ',' 分割用户输入字符串
        String[] parts = userInput.split(",");
        if (parts.length != 3) {
            System.out.println("参数个数错误");
            return;
        }

        // 获得方法名
        String methodName = parts[0];

        // 提取包名和类名
        String[] packageAndClass = parts[1].split("\\.");
        if (packageAndClass.length != 2) {
            System.out.println("参数 2 格式错误");
            return;
        }

        // 获得包名和类名
        String packageName = packageAndClass[0];
        String className = packageAndClass[1];

        // 提取深度信息
        String[] depthInfo = parts[2].split("=");
        if (depthInfo.length != 2 || !depthInfo[0].equalsIgnoreCase("depth")) {
            System.out.println("参数 3 格式错误");
            return;
        }

        // 获取深度值
        int depth;
        try {
            depth = Integer.parseInt(depthInfo[1]);
        } catch (NumberFormatException e) {
            System.out.println("参数 3 格式错误");
            return;
        }

        // 创建分析器进行分析
        ProjectAnalyzer projectAnalyzer = new ProjectAnalyzer(packageName);
        projectAnalyzer.analyzeSpecificMethod(methodName, className, depth);
    }
}
