package org.parser;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;

import java.util.ArrayList;

public class Methods {
    private String packageName;
    private String className;
    private String methodName;
    private MethodDeclaration declaration;
    private ArrayList<Methods> callers;
    private ArrayList<Methods> callees;
    public Methods(String packageName, String className, String methodName) {
        this.packageName = packageName;
        this.className = className;
        this.methodName = methodName;
        this.declaration = null;
        callers = new ArrayList<>();
        callees = new ArrayList<>();
    }

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
        return String.format("%s.%s.%s", packageName, className, methodName);
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
}
