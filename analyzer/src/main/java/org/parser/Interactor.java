package org.parser;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.Parameter;

import java.util.ArrayList;
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
                interactor.showName(false);
                System.out.println("再见");
            }
        }
        interactor.closeScanner(); // 关闭 scanner
    }
    private static Interactor instance; // 交互器实例
    private String myName = "Assistant"; // 交互器名字
    private String yourName = "User"; // 用户名字
    private Scanner scanner = new Scanner(System.in); // 用于读取系统输入
    private boolean endFlag = false; // 交互结束标志
    // 私有构造函数
    private Interactor() {}

    // 获取交互器对象（单例模式）
    public static Interactor getInstance() {
        if (instance == null) {
            instance = new Interactor();
        }
        return instance;
    }

    // 展示说话人名字
    private void showName(boolean who) {
        if (who) {
            System.out.print(yourName + ": ");
        } else {
            System.out.print(myName + ": ");
        }
    }

    // 输出提示语
    public void showPrompt() {
        showName(false);
        System.out.println("请选择功能（'1'-分析调用/'2'-分析参数/其他-退出）");
    }

    // 选择功能
    public int functionSelection() {
        showName(true);
        String userInput = scanner.nextLine();
        if (userInput.equals("1")) {
            return 1;
        } else if (userInput.equals("2")) {
            return 2;
        }
        return 0;
    }

    // 调用分析的交互
    public void invocationAnalysis() {
        // 输出提示信息
        showName(false);
        System.out.println("请依次输入方法名、所属类、查找深度，格式如 \"introduction, main.Test, 2\"");

        // 读取用户输入并去除空白符
        showName(true);
        String userInput = scanner.nextLine().replaceAll("\\s","");

        // 用 ',' 分割输入字符串
        String[] parts = userInput.split(",");
        if (parts.length != 3) {
            showName(false);
            System.out.println("参数个数错误");
            return;
        }

        // 获得方法名
        String methodName = parts[0];

        // 提取包名和类名
        String[] packageAndClass = parts[1].split("\\.");
        if (packageAndClass.length < 2) {
            showName(false);
            System.out.println("参数 2 格式错误");
            return;
        }

        // 获得包名和类名
        String packageName = packageAndClass[0];
        String className = packageAndClass[1];

        // 获取深度值
        int depth;
        try {
            depth = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            showName(false);
            System.out.println("参数 3 格式错误");
            return;
        }

        Analyzer.getInstance().analyzeInvocations(packageName, className, methodName, depth);
    }

    // 参数分析的交互
    public void parameterAnalysis() {
        showName(false);
        System.out.println("所有方法的参数来源如下");
        System.out.println("");
        Analyzer.getInstance().analyzeParameter();
        System.out.println("");
    }

    // 终止交互
    public void end() {
        endFlag = true;
    }

    // 交互状态（是否终止）
    public boolean state() {
        return !endFlag;
    }

    // 关闭 scanner
    public void closeScanner() {
        scanner.close();
    }

    // 打印缩进
    private void indent(int spaces) {
        if (spaces < 0) {
            return;
        }
        System.out.print(" ".repeat(spaces));
    }

    // 在参数分析中打印函数参数
    public void printParameter(Parameter parameter) {
        printWithIndent(2, String.format("%s %s: ", parameter.getTypeAsString(), parameter.getNameAsString()));
    }

    // 在参数分析中打印参数来源的表达式
    public <T extends Node> void printExpression(int depth, T expression) {
        int line = expression.getRange().get().begin.line; // 获取实参所在函数
        String fileName = expression.findAncestor(CompilationUnit.class).get().getStorage().get().getPath().getFileName().toString();
        indent(depth * 2);
        System.out.println(String.format("[%s: Line %d of %s]", expression, line, fileName)); // 打印信息
    }

    // 选择重载的交互
    public int chooseOverloadedMethods(ArrayList<Methods> overload) {
        showName(false);
        System.out.println(String.format("请选择一个重载方法（0-%d）", overload.size() - 1));
        for (int i = 0; i < overload.size(); ++i) {
            Methods method = overload.get(i);
            printWithIndent(2, String.format("方法 %d: %s", i, method.getDeclaration().getDeclarationAsString()));
        }
        showName(true);
        if (scanner.hasNextInt()) {
            int choice = Integer.parseInt(scanner.nextLine());
            if (choice >= 0 && choice < overload.size()) {
                return choice;
            }
        } else {
            scanner.nextLine();
        }
        System.out.println("输入无效");
        return -1;
    }

    // 展示统计数据
    public void showStatistics(int classesCount, int methodCount) {
        showName(false);
        System.out.println(String.format("分析完成，共扫描到 %d 个类和 %d 个方法，嘻嘻", classesCount, methodCount));
    }

    // 提示方法不存在
    public void methodNotFount() {
        showName(false);
        System.out.println("方法不存在");
    }

    // 在调用分析中输出提示
    public void invokes() {
        showName(false);
        System.out.println("它调用了这些方法");
    }

    // 在调用分析中输出提示
    public void invoked() {
        showName(false);
        System.out.println("它被这些方法调用了");

    }

    // 打印带有缩进的内容
    public void printWithIndent(int spaces, String content) {
        indent(spaces);
        System.out.println(content);
    }
}
