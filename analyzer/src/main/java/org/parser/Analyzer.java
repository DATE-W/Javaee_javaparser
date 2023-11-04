package org.parser;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import javassist.Loader;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    public void findArgument(Methods callee, int index, int depth) {    // index 表示第几个实参
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

    private void findLastAssignmentsAndDeclarationsWithTargetInIfs(Statement statement, String targetVariableName, List<Node> lastAssignmentsAndDeclarations) {
        if (statement instanceof IfStmt) {
            IfStmt ifStmt = (IfStmt) statement;
            // 对 then 子句递归搜索
            findLastAssignmentsAndDeclarationsWithTargetInIfs(ifStmt.getThenStmt(), targetVariableName, lastAssignmentsAndDeclarations);
            // 对 else 子句递归搜索（如果有的话）
            ifStmt.getElseStmt().ifPresent(elseStmt -> findLastAssignmentsAndDeclarationsWithTargetInIfs(elseStmt, targetVariableName, lastAssignmentsAndDeclarations));
        } else if (statement.isBlockStmt()) {
            // 对块语句中的每一条语句递归搜索
            statement.asBlockStmt().getStatements().forEach(childStmt -> findLastAssignmentsAndDeclarationsWithTargetInIfs(childStmt, targetVariableName, lastAssignmentsAndDeclarations));
        } else {
            // 非 if 语句块，寻找其中的最后一个赋值表达式或变量定义，且右值为targetVariableName
            Optional<Node> lastAssignmentOrDeclaration = statement.findAll(Node.class).stream()
                    .filter(node ->
                            (node instanceof AssignExpr && ((AssignExpr) node).getValue().toString().equals(targetVariableName)) ||
                                    (node instanceof VariableDeclarationExpr && ((VariableDeclarationExpr) node).getVariables().stream().anyMatch(var -> var.getInitializer().isPresent() && var.getInitializer().get().toString().equals(targetVariableName)))
                    )
                    .reduce((first, second) -> second); // 取最后一个
            lastAssignmentOrDeclaration.ifPresent(lastAssignmentsAndDeclarations::add);
        }
    }

    // 找到实参的来源
    public void findSource(Methods method, Expression variable, int depth) {
        int lastLine = -1; // 用于记录最后的行号
        Expression lastExpression = null; // 用于记录最后的表达式

        // 获取所有的赋值表达式和变量声明
        List<Node> assignmentsAndDeclarations = new ArrayList<>();
        assignmentsAndDeclarations.addAll(method.findByType(AssignExpr.class));
        assignmentsAndDeclarations.addAll(method.findByType(VariableDeclarator.class));

        for (Node node : assignmentsAndDeclarations) {
            if (node instanceof AssignExpr) {
                AssignExpr assignExpr = (AssignExpr) node;
                int assignExprLine = assignExpr.getRange().get().begin.line;
                // 判断变量名和行号条件
                if (assignExpr.getTarget().isNameExpr() &&
                        assignExpr.getTarget().asNameExpr().getNameAsString().equals(variable.toString()) &&
                        assignExprLine <= variable.getRange().get().begin.line &&
                        assignExprLine > lastLine) {

                    lastLine = assignExprLine;
                    lastExpression = assignExpr.getValue();
                }
            } else if (node instanceof VariableDeclarator) {
                VariableDeclarator declarator = (VariableDeclarator) node;
                int declaratorLine = declarator.getRange().get().begin.line;
                // 判断变量名和行号条件
                if (declarator.getNameAsString().equals(variable.toString()) &&
                        declaratorLine <= variable.getRange().get().begin.line &&
                        declaratorLine > lastLine &&
                        declarator.getInitializer().isPresent()) {

                    lastLine = declaratorLine;
                    lastExpression = declarator.getInitializer().get();
                }
            }
        }

        // 输出结果
        Interactor.getInstance().indent(depth * 2); // 缩进

        // 不空则考虑下一步
        if (lastExpression != null) {
            // 如果是个普通变量，就继续跳
            if (lastExpression.isNameExpr()) {
                Interactor.getInstance().printExpression(lastExpression);
                findSource(method, lastExpression, depth + 1);
                return;
            }
        } else { // 如果没有符合要求的声明/赋值语句，就去寻找形参
            ArrayList<Parameter> parameters = method.getParameters();
            for (int i = 0; i < parameters.size(); i++) { // 在函数声明中找形参
                if (parameters.get(i).getNameAsString().equals(variable.toString())) {
                    Interactor.getInstance().printExpression(parameters.get(i).getName()); // 输出形参信息
                    findArgument(method, i, depth + 1); // 继续寻找实参
                    break;
                }
            }
            return;
        }

        // 打印 target
        Interactor.getInstance().printExpression(lastExpression);
    }
}

