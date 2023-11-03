package org.parser;

import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import javassist.Loader;

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
            Interactor.getInstance().indent(depth * 2);
            System.out.println(current.getInfoString(depth));
        }
        ArrayList<Methods> list = direction ? current.getCallees() : current.getCallers();
        for (Methods next : list) {
            searchInvocation(next, direction, depth + 1, upper);
        }
    }
    // 分析所有函数的参数来源
    public void analyzeParameter() {
        for (Methods method : invocations.getAllMethods()) {
            if (method.isLeaf() || method.isRoot()) { // 跳过系统函数和 main 函数
                continue;
            }
            System.out.println(method.getIdentifier() + ": ");
            ArrayList<Parameter> parameters = method.getParameters();
            for (int i = 0; i < parameters.size(); i++) {
                Interactor.getInstance().indent(2);
                Interactor.getInstance().printParameter(parameters.get(i));
                findArgument(method, i, 2);
            }
        }
    }

    // 找到方法所有调用处的实参
    public void findArgument(Methods callee, int index, int depth) {
        String qualifiedSignature = callee.qualifiedSignature();
        for (Methods caller : callee.getCallers()) { // 遍历调用者
            // 分析调用者的声明
            for (MethodCallExpr methodCallExpr : caller.findByType(MethodCallExpr.class)) {
                // 判断调用的是不是自己

                /* 注意：这里在完成 F2 之后需要继续完善 */

                String target = methodCallExpr.resolve().getQualifiedSignature();
                if (!target.equals(qualifiedSignature)) {
                    continue;
                }

                // 获取实参并打印
                Expression argument = methodCallExpr.getArguments().get(index); // 根据索引获取实参
                Interactor.getInstance().indent(depth * 2);
                Interactor.getInstance().printExpression(argument);

                // 实参是字面量则结束
                if (argument.isLiteralExpr()) {
                    return;
                }

                // 继续找右值为字面量的赋值语句或声明语句
                findSource(caller, argument, depth + 1);
            }
        }
    }

    // 找到实参的来源
    public void findSource(Methods method, Expression argument, int depth) {
        // 选择右值为字面量的、出现在实参前的、位置最靠后的语句
        Expression target = null;
        int targetLine = 0;

        // 寻找赋值表达式
        for (AssignExpr assignExpr : method.findByType(AssignExpr.class)) {
            if (!assignExpr.getValue().isLiteralExpr()) {
                continue;
            }
            int assignExprLine = assignExpr.getRange().get().begin.line;
            if (assignExprLine > argument.getRange().get().begin.line) {
                continue;
            }
            if (assignExprLine > targetLine) {
                targetLine = assignExprLine;
                target = assignExpr;
            }
        }

        // 寻找变量声明语句
        for (VariableDeclarator declarator : method.findByType(VariableDeclarator.class)) {
            if (!declarator.getInitializer().get().isLiteralExpr()) {
                continue;
            }
            int declaratorLine = declarator.getRange().get().begin.line;
            if (declaratorLine > argument.getRange().get().begin.line) {
                continue;
            }
            if (declaratorLine > targetLine) {
                targetLine = declaratorLine;
                target = declarator.getInitializer().get().asLiteralExpr();
            }
        }

        // 输出结果
        Interactor.getInstance().indent(depth * 2); // 缩进
        if (target == null) { // 没有符合要求的赋值语句
            ArrayList<Parameter> parameters = method.getParameters();
            for (int i = 0; i < parameters.size(); i++) { // 在函数声明中找形参
                if (parameters.get(i).getNameAsString().equals(argument.toString())) {
                    Interactor.getInstance().printExpression(parameters.get(i).getName()); // 输出形参信息
                    findArgument(method, i, depth + 1); // 继续寻找实参
                    break;
                }
            }
            return;
        }
        Interactor.getInstance().printExpression(target);
    }

}
