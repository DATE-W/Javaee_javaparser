package org.parser;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;

import java.util.ArrayList;

public class Methods {
    private String packageName;
    private String className;
    private String methodName;
    // 要么有 declaration 要么有 typeList
    private MethodDeclaration declaration;
    private ArrayList<String> typeList;
    private ArrayList<Methods> callers;
    private ArrayList<Methods> callees;

    public static String constructIdentifier(String packageName, String className, String methodName, NodeList<Parameter> params) {
        ArrayList<String> typeList = new ArrayList<>();
        for (Parameter param : params) {
            typeList.add(param.getType().asString());
        }
        return constructIdentifier(packageName, className, methodName, typeList);
    }
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

    public void addCaller(Methods method) {
        if (!callers.contains(method)) {
            callers.add(method);
        }
    }
    public void addCallee(Methods method) {
        if (!callees.contains(method)) {
            callees.add(method);
        }
    }
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
    public String getInfoString(int depth) {
        return String.format("[%s, %s.%s, depth=%d]", methodName, packageName, className, depth);
    }
    public boolean isLeaf() {
        return declaration == null;
    }
    public boolean isRoot() {
        return methodName.equals("main");
    }
    public ArrayList<Methods> getCallers() {
        return callers;
    }
    public ArrayList<Methods> getCallees() {
        return callees;
    }
    public ArrayList<Parameter> getParameters() {
        return new ArrayList<>(declaration.getParameters());
    }
    public <T extends Node> ArrayList<T> findByType(Class<T> type) {
        return new ArrayList<>(declaration.findAll(type));
    }
    public String qualifiedSignature() {
        return declaration.resolve().getQualifiedSignature();
    }

    public MethodDeclaration getDeclaration() {
        return declaration;
    }
    public String getMethodName() {
        return methodName;
    }
    public String getPackageName() {
        return packageName;
    }
    public String getClassName() {
        return className;
    }
}
