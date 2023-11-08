package org.parser;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;

import java.util.*;

public class Analyzer {
    private static Analyzer instance; // 分析器实例
    private ParsersAdapter adapter; // 适配器
    private Invocations invocations; // 关系图

    // 构造函数
    private Analyzer() {
        invocations = new Invocations(); // 创建关系图
        adapter = new ParsersAdapter("src\\main\\java"); // 创建适配器
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
        Interactor.getInstance().showStatistics(adapter.countClasses(), invocations.getAllMethods().size());
    }

    // 寻找指定方法的所有重载
    private ArrayList<Methods> findFunctionOverload(String prefix, String methodName) {
        ArrayList<Methods> ret = new ArrayList<>();
        for (Methods method : invocations.getAllMethods()) {
            if (method.getMethodName().equals(methodName) && method.getPrefix().equals(prefix)) {
                ret.add(method);
            }
        }
        return ret;
    }

    // 分析方法调用关系
    public void analyzeInvocations(String prefix, String methodName, int depth) {
        // 查找所有候选方法
        ArrayList<Methods> overload = findFunctionOverload(prefix, methodName);
        if (overload.isEmpty()) {
            Interactor.getInstance().methodNotFount();
            return;
        }

        // 选择目标方法
        Methods target = overload.get(0); // 目标方法
        if (overload.size() > 1) { // 拥有多个重载
            int selection = Interactor.getInstance().chooseOverloadedMethods(overload); // 用户选择重载
            if (selection == -1) { // 用户输入参数错误
                return;
            }
            target = overload.get(selection); // 获取用户所选方法
        }

        // 开始分析
        Interactor.getInstance().invoked();
        searchInvocation(target, false, 0, depth); // 反向 DFS
        Interactor.getInstance().printWithIndent(0, "");
        Interactor.getInstance().invokes();
        searchInvocation(target, true, 0, depth); // 正向 DFS
        Interactor.getInstance().printWithIndent(0, "");
    }

    // 深度优先搜索
    public void searchInvocation(Methods current, boolean direction, int depth, int upper) {
        if (depth > upper) {
            return;
        }
        if (depth > 0) {
            Interactor.getInstance().printWithIndent(depth * 2, current.getInfoString(depth));
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
            Interactor.getInstance().printWithIndent(0, method.getIdentifier() + ": ");
            ArrayList<Parameter> parameters = method.getParameters();
            for (int i = 0; i < parameters.size(); i++) {
                Interactor.getInstance().printParameter(parameters.get(i));
                findArgument(method, i, 2);
            }
            Interactor.getInstance().printWithIndent(0, "");
        }
    }

    // 找到方法所有调用处的实参
    public void findArgument(Methods callee, int index, int depth) { // index 表示第几个实参
        if (depth > 10) {
            return;
        }
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
                Interactor.getInstance().printExpression(depth, argument);

                // 实参是字面量则结束
                if (argument.isLiteralExpr()) {
                    continue;
                }

                // 继续找右值为字面量的赋值语句或声明语句
                findSource(caller, argument, depth + 1);
            }
        }
    }

    // 对于一个代码块，去用 findLastAssignmentOrDeclaration 函数递归的查找
    private void findLastAssignmentsAndDeclarationsWithTargetInIfs(List<Statement> statements, String targetVariableName, List<Node> lastAssignmentsAndDeclarations) {
        for (Statement statement : statements) {
            if (statement instanceof IfStmt) {
                IfStmt ifStmt = (IfStmt) statement;
                // 对 then 子句递归搜索
                Statement thenStmt = ifStmt.getThenStmt();
                // 提取里面的 then 语句，进行递归
                if (thenStmt.isBlockStmt()) {   // 如果 then 后面是代码块
                    BlockStmt blockStmt = thenStmt.asBlockStmt();
                    findLastAssignmentsAndDeclarationsWithTargetInIfs(blockStmt.getStatements(), targetVariableName, lastAssignmentsAndDeclarations);
                } else        // 如果 then 后面是单条语句
                {
                    List<Statement> thenStatements = Collections.singletonList(thenStmt);
                    findLastAssignmentsAndDeclarationsWithTargetInIfs(thenStatements, targetVariableName, lastAssignmentsAndDeclarations);
                }
                // 如果有 else 语句，就对 else 子句进行处理
                if (ifStmt.getElseStmt().isPresent()) {
//                    System.out.println("ok");
                    Statement elseStmt = ifStmt.getElseStmt().get();
                    // 提取里面的 else 语句，进行递归
                    if (elseStmt.isBlockStmt()) {   // 如果 else 后面是代码块
                        BlockStmt elseBlockStmt = elseStmt.asBlockStmt();
                        findLastAssignmentsAndDeclarationsWithTargetInIfs(elseBlockStmt.getStatements(), targetVariableName, lastAssignmentsAndDeclarations);
                    } else {   // 如果 else 后面是单条语句
                        List<Statement> elseStatements = Collections.singletonList(elseStmt);
                        findLastAssignmentsAndDeclarationsWithTargetInIfs(elseStatements, targetVariableName, lastAssignmentsAndDeclarations);
                    }
                }
            } else if (statement instanceof ForStmt) {
                ForStmt forStmt = (ForStmt) statement;
                // 处理 for 循环语句
                Statement body = forStmt.getBody();
                if (body.isBlockStmt()) {
                    BlockStmt blockStmt = body.asBlockStmt();
                    findLastAssignmentsAndDeclarationsWithTargetInIfs(blockStmt.getStatements(), targetVariableName, lastAssignmentsAndDeclarations);
                } else {
                    List<Statement> bodyStatements = Collections.singletonList(body);
                    findLastAssignmentsAndDeclarationsWithTargetInIfs(bodyStatements, targetVariableName, lastAssignmentsAndDeclarations);
                }
            } else if (statement instanceof WhileStmt) {
                WhileStmt whileStmt = (WhileStmt) statement;
                // 处理 while 循环语句
                Statement body = whileStmt.getBody();
                if (body.isBlockStmt()) {
                    BlockStmt blockStmt = body.asBlockStmt();
                    findLastAssignmentsAndDeclarationsWithTargetInIfs(blockStmt.getStatements(), targetVariableName, lastAssignmentsAndDeclarations);
                } else {
                    List<Statement> bodyStatements = Collections.singletonList(body);
                    findLastAssignmentsAndDeclarationsWithTargetInIfs(bodyStatements, targetVariableName, lastAssignmentsAndDeclarations);
                }
            }
        }
        findLastAssignmentOrDeclaration(statements, targetVariableName, lastAssignmentsAndDeclarations);
    }

    // 对一个指定的代码块，去找里面最后一个声明或赋值语句
    private void findLastAssignmentOrDeclaration(List<Statement> statements, String targetVariableName, List<Node> lastAssignmentsAndDeclarations) {
        // 这里使用stream来找到最后的赋值或声明
        Optional<Node> lastAssignmentOrDeclaration = Optional.empty();
        // 从上往下找，按照顺序会默认找到最后一个
        for (Statement statement : statements) {
            // 在这里跳过条件判断代码块
            if (statement instanceof IfStmt || statement instanceof WhileStmt || statement instanceof ForStmt) {
                continue;
            }
            // 处理非条件判断语句代码块，取最后一个
            for (Node node : statement.findAll(Node.class)) {
                if (node instanceof AssignExpr) {
                    AssignExpr assignExpr = (AssignExpr) node;
                    if (assignExpr.getTarget().isNameExpr()) {
                        if (assignExpr.getTarget().asNameExpr().getNameAsString().equals(targetVariableName)) {
                            lastAssignmentOrDeclaration = Optional.of(node);
                        }
                    }
                } else if (node instanceof VariableDeclarator) {
                    VariableDeclarator variableDeclarator = (VariableDeclarator) node;
                    if (variableDeclarator.getNameAsString().equals(targetVariableName) &&
                            variableDeclarator.getInitializer().isPresent()) {
                        lastAssignmentOrDeclaration = Optional.of(node);
                    }
                }
            }
        }
        // 如果不为空，就加入传入的数组里面
        lastAssignmentOrDeclaration.ifPresent(lastAssignmentsAndDeclarations::add);
    }

    // 找到实参的来源
    public void findSource(Methods method, Expression variable, int depth) {
        List<Node> lastAssignmentsAndDeclarations = new ArrayList<>();
        List<Expression> lastExpressions = new ArrayList<>();
        findLastAssignmentsAndDeclarationsWithTargetInIfs(method.getDeclaration().getBody().get().getStatements(), variable.toString(), lastAssignmentsAndDeclarations);
        // 将这些表达式的右侧都提取出来
        for (Node node : lastAssignmentsAndDeclarations) {
            if (node instanceof AssignExpr) {
                AssignExpr assignExpr = (AssignExpr) node;
                lastExpressions.add(assignExpr.getValue());
            }
            if (node instanceof VariableDeclarator) {
                VariableDeclarator declarator = (VariableDeclarator) node;
                lastExpressions.add(declarator.getInitializer().get());
            }
        }
        if (!lastExpressions.isEmpty()) {
            for (Expression lastExpression : lastExpressions)   // 这里面都是不为空的
            {
                if (lastExpression.isNameExpr()) {      // 是变量，继续查找
                    Interactor.getInstance().printExpression(depth, lastExpression);
                    findSource(method, lastExpression, depth + 1);
                } else {  // 停止查找
                    Interactor.getInstance().printExpression(depth, lastExpression);
                }
            }
        } else {
            // 如果没有符合要求的声明/赋值语句，就去寻找形参
            ArrayList<Parameter> parameters = method.getParameters();
            for (int i = 0; i < parameters.size(); i++) { // 在函数声明中找形参
                if (parameters.get(i).getNameAsString().equals(variable.toString())) {
                    Interactor.getInstance().printExpression(depth, parameters.get(i).getName()); // 输出形参信息
                    findArgument(method, i, depth + 1); // 继续寻找实参
                    break;
                }
            }
        }
    }
}

