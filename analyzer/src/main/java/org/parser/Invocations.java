package org.parser;

import com.github.javaparser.ast.body.MethodDeclaration;

import java.util.*;

public class Invocations {
    private Map<String, Methods> table; // 从标识符到方法结点的映射

    // 构造函数
    public Invocations() {
        table = new HashMap<>();
    }

    // 添加结点，适用于找得到声明的用户自定义函数
    public void addNode(String packageName, String className, MethodDeclaration declaration) {
        Methods node = new Methods(packageName, className, declaration);
        if (!table.containsKey(node.getIdentifier())) {
            table.put(node.getIdentifier(), node);
        }
    }

    // 添加结点，适用于找不到声明的系统函数，需要传入参数类型列表
    public void addNode(String packageName, String className, String methodName, ArrayList<String> typeList) {
        Methods node = new Methods(packageName, className, methodName, typeList);
        if (!table.containsKey(node.getIdentifier())) {
            table.put(node.getIdentifier(), node);
        }
    }

    // 添加调用关系，即建边
    public void addEdge(Methods caller, Methods callee) {
        caller.addCallee(callee);
        callee.addCaller(caller);
    }

    // 根据标识符在哈希表中寻找方法结点
    public Methods findMethod(String identifier) {
        if (table.containsKey(identifier)) {
            return table.get(identifier);
        }
        return null;
    }

    // 返回包含所有结点的列表
    public ArrayList<Methods> getAllMethods() {
        return new ArrayList<>(table.values());
    }
}
