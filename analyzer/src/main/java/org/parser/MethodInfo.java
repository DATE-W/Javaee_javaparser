package org.parser;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.ArrayList;
import java.util.List;

public class MethodInfo {
    private final MethodDeclaration declaration;
    private final List<MethodInfo> calledMethods;
    private final List<MethodInfo> methodsCallingThis;

    public MethodInfo(MethodDeclaration declaration) {
        this.declaration = declaration;
        this.calledMethods = new ArrayList<>();
        this.methodsCallingThis = new ArrayList<>();
    }

    public void addCalledMethod(MethodInfo method) {
        if (!calledMethods.contains(method)) {
            this.calledMethods.add(method);
        }
    }

    public void addMethodCallingThis(MethodInfo method) {
        if (!methodsCallingThis.contains(method)) {
            this.methodsCallingThis.add(method);
        }
    }

    public String getMethodName() {
        return declaration.getNameAsString();
    }

    public List<MethodInfo> getCalledMethods() {
        return calledMethods;
    }

    public List<MethodInfo> getMethodsCallingThis() {
        return methodsCallingThis;
    }

    public void analyze(List<MethodInfo> allMethods) {
        List<MethodCallExpr> methodCalls = declaration.findAll(MethodCallExpr.class);

        for (MethodCallExpr methodCall : methodCalls) {
            String calledMethodName = methodCall.getNameAsString();

            for (MethodInfo methodInfo : allMethods) {
                if (methodInfo.getMethodName().equals(calledMethodName)) {
                    this.addCalledMethod(methodInfo);
                    methodInfo.addMethodCallingThis(this);
                }
            }
        }
    }

    public String getInvokes(int depth, int currentDepth, String packageName) {
        List<String> result = new ArrayList<>();
        for (MethodInfo method : calledMethods) {
            String newEntry = "[" + method.getMethodName() + ", " + packageName + "." + method.declaration.findAncestor(ClassOrInterfaceDeclaration.class).get().getNameAsString() + " (depth:" + currentDepth + ")]";

            boolean isAdded = false;
            for (int i = 0; i < result.size(); i++) {
                String existingEntry = result.get(i);
                if (existingEntry.startsWith("[" + method.getMethodName() + ", " + packageName + ".")) {
                    int existingDepth = Integer.parseInt(existingEntry.split("depth:")[1].split("\\)")[0]);
                    if (currentDepth > existingDepth) {
                        result.set(i, newEntry);
                    }
                    isAdded = true;
                    break;
                }
            }

            if (!isAdded) {
                result.add(newEntry);
            }

            if (depth > currentDepth) {
                result.add(method.getInvokes(depth, currentDepth + 1, packageName));
            }
        }
        return String.join("\n", result);
    }

    public String getInvokedBy(int depth, int currentDepth, String packageName) {
        List<String> result = new ArrayList<>();
        for (MethodInfo method : methodsCallingThis) {
            String newEntry = "[" + method.getMethodName() + ", " + packageName + "." + method.declaration.findAncestor(ClassOrInterfaceDeclaration.class).get().getNameAsString() + " (depth:" + currentDepth + ")]";

            boolean isAdded = false;
            for (int i = 0; i < result.size(); i++) {
                String existingEntry = result.get(i);
                if (existingEntry.startsWith("[" + method.getMethodName() + ", " + packageName + ".")) {
                    int existingDepth = Integer.parseInt(existingEntry.split("depth:")[1].split("\\)")[0]);
                    if (currentDepth > existingDepth) {
                        result.set(i, newEntry);
                    }
                    isAdded = true;
                    break;
                }
            }

            if (!isAdded) {
                result.add(newEntry);
            }

            if (depth > currentDepth) {
                result.add(method.getInvokedBy(depth, currentDepth + 1, packageName));
            }
        }
        return String.join("\n", result);
    }




}
