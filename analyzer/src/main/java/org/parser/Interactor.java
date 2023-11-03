package org.parser;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;

import java.util.Scanner;
public class Interactor {
    // 主函数
    public static void main(String[] args) {
        Interactor interactor = Interactor.getInstance(); // 创建交互器
        Analyzer.getInstance().parseDirectory(); // 创建分析器并扫描目录
        while (interactor.state()) { // 交互是否继续
            interactor.showPrompt(); // 显示提示词
            int selected = interactor.functionSelection(); // 选择功能
            if (selected == 1) { // 分析调用关系
                interactor.invocationAnalysis();
            } else if (selected == 2) { // 分析参数来源
                interactor.parameterAnalysis();
            } else if (selected == 0) {
                interactor.end(); // 结束交互
                System.out.println("程序结束");
            }
        }
        interactor.closeScanner(); // 关闭 scanner
    }
    private static Interactor instance; // 交互器实例
    private Scanner scanner = new Scanner(System.in); // 用于读取系统输入
    private boolean endFlag = false; // 交互结束标志
    private Interactor() {}
    // 获取交互器对象（单例模式）
    public static Interactor getInstance() {
        if (instance == null) {
            instance = new Interactor();
        }
        return instance;
    }
    // 输出提示语
    public void showPrompt() {
        System.out.println("输入数字：1-分析调用 2-分析参数 其他-退出");
    }
    // 选择功能
    public int functionSelection() {
        String userInput = scanner.nextLine();
        if (userInput.equals("1")) {
            return 1;
        } else if (userInput.equals("2")) {
            return 2;
        }
        return 0;
    }
    public void invocationAnalysis() {
        System.out.println("请输入方法信息（格式如：introduction, main.Test, depth=2）："); // 输出提示信息
        // 读取用户输入并去除空白符
        String userInput = scanner.nextLine().replaceAll("\\s","");

        // 用 ',' 分割输入字符串
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

        Analyzer.getInstance().analyzeInvocations(packageName, className, methodName, depth);
    }
    public void parameterAnalysis() {
        Analyzer.getInstance().analyzeParameter();
    }
    public void end() {
        endFlag = true;
    }
    public boolean state() {
        return !endFlag;
    }
    public void closeScanner() {
        scanner.close();
    }
    public void indent(int spaces) {
        if (spaces < 0) {
            return;
        }
        System.out.print(" ".repeat(spaces));
    }

    public void printParameter(Parameter parameter) {
        System.out.println(String.format("%s %s: ", parameter.getTypeAsString(), parameter.getNameAsString()));
    }

    public void printExpression(Expression expression) {
        int line = expression.getRange().get().begin.line; // 获取实参所在函数
        String fileName = expression.findAncestor(CompilationUnit.class).get().getStorage().get().getPath().getFileName().toString();
        System.out.println(String.format("[%s: Line %d of %s]", expression, line, fileName)); // 打印信息
    }
}
