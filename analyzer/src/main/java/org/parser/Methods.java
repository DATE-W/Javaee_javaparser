package org.parser;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;

import java.util.ArrayList;

public class Methods {
    private String packageName; // 方法包名
    private String className; // 方法类名
    private String methodName; // 方法名

    // 所有方法要么有 declaration 要么有 typeList

    private MethodDeclaration declaration; // 方法的声明
    private ArrayList<String> typeList; // 方法的参数类型列表
    private ArrayList<Methods> callers; // 方法调用者列表
    private ArrayList<Methods> callees; // 被方法调用者列表

    // 根据方法信息构造方法的标识符
    public static String constructIdentifier(String packageName, String className, String methodName, NodeList<Parameter> params) {
        ArrayList<String> typeList = new ArrayList<>();
        for (Parameter param : params) {
            typeList.add(param.getType().asString());
        }
        return constructIdentifier(packageName, className, methodName, typeList);
    }

    // 根据方法的信息构造方法的标识符
    public static String constructIdentifier(String packageName, String className, String methodName, ArrayList<String> typeList) {
        return String.format("%s.%s.%s(%s)", packageName, className, methodName, String.join(", ", typeList));
    }

    // 由包名、类名、方法名构造，适用于找不到声明的系统函数
    public Methods(String packageName, String className, String methodName, ArrayList<String> typeList) {
        this.packageName = packageName;
        this.className = className;
        this.methodName = methodName;
        this.declaration = null;
        this.typeList = typeList;
        callers = new ArrayList<>();
        callees = new ArrayList<>();
    }

    // 由包名、类名、方法声明构造，适用于用户编写的自定义函数
    public Methods(String packageName, String className, MethodDeclaration declaration) {
        this.packageName = packageName;
        this.className = className;
        this.declaration = declaration;
        this.methodName = declaration.getNameAsString();
        callers = new ArrayList<>();
        callees = new ArrayList<>();
    }

    // 添加调用者
    public void addCaller(Methods method) {
        if (!callers.contains(method)) {
            callers.add(method);
        }
    }

    // 添加被调用者
    public void addCallee(Methods method) {
        if (!callees.contains(method)) {
            callees.add(method);
        }
    }

    // 获取方法的标识符
    public String getIdentifier() {
        if (declaration == null) { // 针对找不到声明的系统函数
            return constructIdentifier(packageName, className, methodName, typeList);
        }
        // 针对带有声明信息的用户自定义函数
        ArrayList<String> typeList = new ArrayList<>();
        for (Parameter param : declaration.getParameters()) { // 将所有参数类型加入列表
            typeList.add(param.getType().asString());
        }
        return String.format("%s.%s.%s(%s)", packageName, className, methodName, String.join(", ", typeList));
    }

    // 获取用于打印的信息串
    public String getInfoString(int depth) {
        return String.format("[%s, %s.%s, depth=%d]", methodName, packageName, className, depth);
    }

    // 是否系统函数
    public boolean isLeaf() {
        return declaration == null;
    }

    // 是否 main 函数
    public boolean isRoot() {
        return methodName.equals("main");
    }

    // 返回调用者列表
    public ArrayList<Methods> getCallers() {
        return callers;
    }

    // 返回被调用者列表
    public ArrayList<Methods> getCallees() {
        return callees;
    }

    // 获取方法的所有参数
    public ArrayList<Parameter> getParameters() {
        return new ArrayList<>(declaration.getParameters());
    }

    // 在 AST 中寻找方法声明的某种类型的子节点
    public <T extends Node> ArrayList<T> findByType(Class<T> type) {
        return new ArrayList<>(declaration.findAll(type));
    }

    // 获取 qualifiedSignature
    public String qualifiedSignature() {
        return declaration.resolve().getQualifiedSignature();
    }

    // 获取方法的声明
    public MethodDeclaration getDeclaration() {
        return declaration;
    }

    // 获取方法名
    public String getMethodName() {
        return methodName;
    }

    // 获取方法包名
    public String getPackageName() {
        return packageName;
    }

    // 获取方法类名
    public String getClassName() {
        return className;
    }
}
