package org.parser;

import java.util.ArrayList;

public class Analyzer {
    private static Analyzer instance; // 分析器实例
    private ParsersAdapter adapter; // 适配器
    private Invocations invocations; // 关系图
    // 构造函数
    private Analyzer() {
        invocations = new Invocations(); // 创建关系图
        adapter = new ParsersAdapter("src/main/java"); // 创建适配器
    }

    // 获取分析器对象（单例模式）
    public static Analyzer getInstance() {
        if (instance == null) {
            instance = new Analyzer();
        }
        return instance;
    }
    // 扫描项目目录
    public void parseDirectory() {
        adapter.analyzeAllFiles(invocations); // 分析所有文件并将结果存入图中
        // invocations.printAllMethods(); 输出所有方法结点
    }
    // 分析方法调用关系
    public void analyzeInvocations(String packageName, String className, String methodName, int depth) {
        String identifier = String.format("%s.%s.%s", packageName, className, methodName); // 目标方法的标识符
        Methods target = invocations.findMethod(identifier); // 找到目标方法结点
        if (target == null) {
            System.out.println("方法不存在");
            return;
        }
        System.out.println("It is invoked by the following: ");
        searchInvocation(target, false, 0, depth); // 反向 DFS
        System.out.println("It invokes the following: ");
        searchInvocation(target, true, 0, depth); // 正向 DFS
    }
    // 深度优先搜索
    public void searchInvocation(Methods current, boolean direction, int depth, int upper) {
        if (depth > upper) {
            return;
        }
        if (depth > 0) {
            System.out.print(" ".repeat(depth * 2));
            System.out.println(current.getInfoString(depth));
        }
        ArrayList<Methods> list = direction ? current.getCallees() : current.getCallers();
        for (Methods next : list) {
            searchInvocation(next, direction, depth + 1, upper);
        }
    }
    // 分析所有函数的参数来源
    public void analyzeParameter() {
        // ...
    }

}
