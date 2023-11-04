package org.parser;

import com.github.javaparser.ast.type.Type;

import java.util.*;
import java.util.stream.IntStream;

class MethodCallAnalyzer {
    public static void main(String[] args) {
        System.out.println("请依次输入方法名、所属类、查找深度（格式如：introduction, main.Test, 2）："); // 输出提示信息
        String userInput = GlobalVariables.getScanner().nextLine(); // 从 scanner 中获取用户输入字符串
        userInput = userInput.replaceAll("\\s", ""); // 去除空白符

        try {
            userInputFormat input=resolveUserInput(userInput);
            MethodCallAnalyzer analyzer = new MethodCallAnalyzer(); // 创建 analyzer
            analyzer.analyzeMethodCall(input); // 分析用户输入
        }catch (UserInputException e){
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }


    private static userInputFormat resolveUserInput(String userInput) throws UserInputException {
        // 用 ',' 分割用户输入字符串
        String[] parts = userInput.split(",");
        if (parts.length != 3) {
            throw new UserInputException("参数个数错误");
        }

        // 获得方法名
        String methodName = parts[0];

        // 提取包名和类名
        String[] packageAndClass = parts[1].split("\\.");
        //JieChu: 包可能有子包，所以这里是<2
        if (packageAndClass.length < 2) {
            throw new UserInputException("参数 2 格式错误");
        }

        // 获得包名和类名
        int lastDotIndex = parts[1].lastIndexOf('.');
        String packageName = parts[1].substring(0, lastDotIndex);
        String className = parts[1].substring(lastDotIndex + 1);

        int depth;
        try {
            depth = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            throw new UserInputException("参数 3 格式错误");
        }

        return new userInputFormat(methodName, packageName, className, depth);
    }


    //自定义记录
    private record userInputFormat(String methodName, String packageName, String className, int depth) {}

    private void analyzeMethodCall(userInputFormat userInput) {
        // 创建分析器进行分析
        ProjectAnalyzer projectAnalyzer = new ProjectAnalyzer(userInput.packageName);

        //JieChu: 判断用户输入的方法是否存在重载
        Map.Entry<Boolean, List<MethodInfo>> functionOverloadChecked = projectAnalyzer.checkFunctionOverload(userInput.methodName, userInput.className);
        //若有方法重载，则用户选择一个重载的方法，此被选择的方法的参数列表存在变量chosenReloadMethodParams中
        Map<String,Type> chosenReloadMethodParams=null;
        //存在重载
        if(functionOverloadChecked.getKey())
        {
            List<MethodInfo> reloadMethods=functionOverloadChecked.getValue();
            printReloadMethodParams(reloadMethods);

            int choice = getReloadMethodChoice(reloadMethods.size());
            MethodInfo chosenMethod=reloadMethods.get(choice);
            chosenReloadMethodParams=chosenMethod.getParamList();
        }

        //假如没有方法重载，则程序不会提示用户选择重载的方法的参数列表，且传入下面方法的chosenReloadMethodParams参数也会为null
        projectAnalyzer.analyzeSpecificMethod(userInput.methodName, userInput.className, userInput.depth, chosenReloadMethodParams);
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
            if (!GlobalVariables.getScanner().hasNextInt()) {
                System.out.println("请输入有效的数字!");
                GlobalVariables.getScanner().next(); // to discard the invalid input
                continue;
            } else {
                int choice = GlobalVariables.getScanner().nextInt();
                if (choice >= 0 && choice <= numberOfMethods-1) {
                    return choice;
                } else {
                    System.out.println("无效的选择");
                    GlobalVariables.getScanner().nextLine();
                    continue;
                }
            }
        }
    }
}