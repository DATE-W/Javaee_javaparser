package org.parser;

import com.github.javaparser.ast.type.Type;

import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.IntStream;

class MethodCallAnalyzer {
    private static final Scanner scanner = new Scanner(System.in);
    public static void main(String[] args) {
        System.out.println("请输入方法信息（格式如：introduction, main.Test, depth=2）："); // 输出提示信息
        String userInput = scanner.nextLine(); // 从 scanner 中获取用户输入字符串
        userInput = userInput.replaceAll("\\s", ""); // 去除空白符

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
        //JieChu: 判断用户输入的方法是否存在重载
        Map.Entry<Boolean, List<MethodInfo>> functionOverloadChecked = projectAnalyzer.checkFunctionOverload(methodName, className);
        //存在重载
        if(functionOverloadChecked.getKey())
        {
            List<MethodInfo> reloadMethods=functionOverloadChecked.getValue();
            printReloadMethodParams(reloadMethods);
            int choice = getReloadMethodChoice(reloadMethods.size());
            MethodInfo chosenMethod=reloadMethods.get(choice);
            methodName=chosenMethod.getMethodName();
            className=chosenMethod.getClassName();
        }
        projectAnalyzer.analyzeSpecificMethod(methodName, className, depth);
    }

    private void printReloadMethodParams(List<MethodInfo> reloadMethodInfo) {
        IntStream.range(0, reloadMethodInfo.size()).forEach(i -> {
            MethodInfo methodInfo=reloadMethodInfo.get(i);
            Map<String,Type> paramList=methodInfo.getParamList();
            System.out.println("方法 " + i +"  " + methodInfo.getMethodName() + ":");
            paramList.forEach((paramName, type) -> System.out.println("\t参数名: " + paramName + ", 类型: " + type));
        });
    }


    private static int getReloadMethodChoice(int numberOfMethods) {
        System.out.println("请选择一个重载方法 (0-" + (numberOfMethods-1) + "):");
        while(true) {
            if (!scanner.hasNextInt()) {
                System.out.println("请输入有效的数字!");
                scanner.next(); // to discard the invalid input
                continue;
            } else {
                int choice = scanner.nextInt();
                if (choice >= 0 && choice <= numberOfMethods-1) {
                    return choice;
                } else {
                    System.out.println("无效的选择");
                    scanner.nextLine();
                    continue;
                }
            }
        }
    }
}
