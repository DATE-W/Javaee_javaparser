package org.parser;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.Range;

import java.util.Optional;


import java.util.ArrayList;
import java.util.List;

public class MethodInfo {
    private final MethodDeclaration declaration; // 方法声明
    private final List<MethodInfo> calledMethods; // 被当前方法调用的方法列表
    private final List<MethodInfo> methodsCallingThis; // 调用当前方法的方法列表
    private final List<List<ParameterInfo>> invokedParameters;   // 当前方法接受的实际参数

    // 构造函数，初始化MethodInfo对象
    public MethodInfo(MethodDeclaration declaration) {
        this.declaration = declaration;
        this.calledMethods = new ArrayList<>();
        this.methodsCallingThis = new ArrayList<>();
        this.invokedParameters = new ArrayList<>();
    }

    // 添加一个被当前方法调用的方法
    public void addCalledMethod(MethodInfo method) {
        if (!calledMethods.contains(method)) {
            this.calledMethods.add(method);
        }
    }

    // 添加一个调用当前方法的方法
    public void addMethodCallingThis(MethodInfo method) {
        if (!methodsCallingThis.contains(method)) {
            this.methodsCallingThis.add(method);
        }
    }

    // 获取当前方法的名称
    public String getMethodName() {
        return declaration.getNameAsString();
    }

    public String getClassName() {
        return declaration.findAncestor(ClassOrInterfaceDeclaration.class).get().getNameAsString();
    }

    // 分析方法，找出此方法调用了哪些方法，并更新calledMethods和methodsCallingThis列表
    public void analyze(List<MethodInfo> allMethods) {
        // 从当前方法声明中找到所有的方法调用表达式
        List<MethodCallExpr> methodCalls = declaration.findAll(MethodCallExpr.class);

        // 遍历所有找到的方法调用表达式
        for (MethodCallExpr methodCall : methodCalls) {
            try {
                ResolvedMethodDeclaration resolvedMethod = methodCall.resolve();
                String qualifiedName = resolvedMethod.getQualifiedSignature();
                String[] methodCallInfo = qualifiedName.split("\\.");
                String methodClassName = methodCallInfo[1];
                String calledMethodName = methodCall.getNameAsString();

                // 遍历传入的所有方法信息
                for (MethodInfo methodInfo : allMethods) {
                    if (methodInfo.getMethodName().equals(calledMethodName) && methodInfo.getClassName().equals(methodClassName)) {
                        this.addCalledMethod(methodInfo);
                        methodInfo.addMethodCallingThis(this);
                    }
                }

            } catch (Exception e) {
                System.out.println(e);
                System.out.println("无法解析方法调用: " + methodCall);
            }
        }

    }


    // 获取此方法调用的所有方法，并以特定格式输出
    public String getInvokes(int depth, int currentDepth, String packageName) {
        List<String> result = new ArrayList<>(); // 创建一个列表来存储结果字符串

        // 遍历此方法调用的所有方法
        for (MethodInfo method : calledMethods) {
            // 获取被调用方法所在的类名
            String className = method.declaration.findAncestor(ClassOrInterfaceDeclaration.class).get().getNameAsString();

            // 创建一个新条目，包含方法名、包名、类名和当前深度
            String newEntry = "[" + method.getMethodName() + ", " + packageName + "." + className + " (depth:" + currentDepth + ")]";

            boolean isAdded = false; // 标志变量，表示新条目是否已添加到结果列表中

            // 遍历结果列表，检查是否已经存在相同的条目
            for (int i = 0; i < result.size(); i++) {
                String existingEntry = result.get(i); // 获取已存在的条目

                // 如果找到相同的条目（方法名和类名相同）
                if (existingEntry.contains(method.getMethodName() + ", " + packageName + "." + className)) {
                    // 获取已存在条目的深度
                    int existingDepth = Integer.parseInt(existingEntry.split("depth:")[1].split("\\)")[0]);

                    // 如果新条目的深度更大，则用新条目替换旧条目
                    if (currentDepth > existingDepth) {
                        result.set(i, newEntry);
                    }
                    isAdded = true; // 设置标志变量为true，表示新条目已添加
                    break;
                }
            }

            // 如果新条目没有被添加，则将其添加到结果列表中
            if (!isAdded) {
                result.add(newEntry);
            }

            // 如果当前深度小于指定的最大深度，则递归调用此方法
            if (depth > currentDepth) {
                // 获取递归调用的结果，并按行分割
                String invokesEntries = method.getInvokes(depth, currentDepth + 1, packageName);
                if (!invokesEntries.equals("")) {
                    String[] invokesArray = invokesEntries.split("\n");

                    // 将递归调用的结果添加到结果列表中，确保不添加重复的条目
                    for (String invoke : invokesArray) {
                        if (!result.contains(invoke)) {
                            result.add(invoke);
                        }
                    }
                }
            }
        }

        // 将结果列表中的所有条目连接成一个字符串，并用换行符分隔
        return String.join("\n", result);
    }


    // 获取调用此方法的所有方法，并以特定格式输出
    public String getInvokedBy(int depth, int currentDepth, String packageName) {
        List<String> result = new ArrayList<>(); // 创建一个列表来存储结果字符串

        // 遍历所有调用此方法的方法
        for (MethodInfo method : methodsCallingThis) {
            // 获取调用此方法的方法所在的类名
            String className = method.declaration.findAncestor(ClassOrInterfaceDeclaration.class).get().getNameAsString();

            // 创建一个新条目，包含方法名、包名、类名和当前深度
            String newEntry = "[" + method.getMethodName() + ", " + packageName + "." + className + " (depth:" + currentDepth + ")]";

            boolean isAdded = false; // 标志变量，表示新条目是否已添加到结果列表中

            // 遍历结果列表，检查是否已经存在相同的条目
            for (int i = 0; i < result.size(); i++) {
                String existingEntry = result.get(i); // 获取已存在的条目

                // 如果找到相同的条目（方法名和类名相同）
                if (existingEntry.contains(method.getMethodName() + ", " + packageName + "." + className)) {
                    // 获取已存在条目的深度
                    int existingDepth = Integer.parseInt(existingEntry.split("depth:")[1].split("\\)")[0]);

                    // 如果新条目的深度更大，则用新条目替换旧条目
                    if (currentDepth > existingDepth) {
                        result.set(i, newEntry);
                    }
                    isAdded = true; // 设置标志变量为true，表示新条目已添加
                    break;
                }
            }

            // 如果新条目没有被添加，则将其添加到结果列表中
            if (!isAdded) {
                result.add(newEntry);
            }

            // 如果当前深度小于指定的最大深度，则递归调用此方法
            if (depth > currentDepth) {
                // 获取递归调用的结果，并按行分割
                String invokedByEntries = method.getInvokedBy(depth, currentDepth + 1, packageName);
                if (!invokedByEntries.equals("")) {
                    String[] invokedByArray = invokedByEntries.split("\n");

                    // 将递归调用的结果添加到结果列表中，确保不添加重复的条目
                    for (String invokedBy : invokedByArray) {
                        if (!result.contains(invokedBy)) {
                            result.add(invokedBy);
                        }
                    }
                }
            }
        }

        // 将结果列表中的所有条目连接成一个字符串，并用换行符分隔
        return String.join("\n", result);
    }


    // 找到某个方法获得的参数
    public void getInvokedParameters() {
        // 遍历所有调用此方法的方法
        for (MethodInfo method : methodsCallingThis) {
            // 从每个方法中找到所有的方法调用表达式
            List<MethodCallExpr> methodCalls = method.declaration.findAll(MethodCallExpr.class);
            // 遍历找到的方法调用表达式
            for (MethodCallExpr methodCall : methodCalls) {
                try {
                    // 检查这个方法调用表达式是否是对当前方法的调用
                    ResolvedMethodDeclaration resolvedMethod = methodCall.resolve();
                    if (resolvedMethod.getQualifiedSignature().equals(this.declaration.resolve().getQualifiedSignature())) {
                        List<ParameterInfo> parameterInfoList = new ArrayList<>();

                        // 获取并存储实际传递的参数
                        List<Expression> arguments = methodCall.getArguments();
//                        if (!arguments.isEmpty()) {
//                            System.out.println("调用 " + getMethodName() + " 的参数:");
//                        }

                        for (Expression argument : arguments) {
                            // 获取变量名
                            String variableName = argument.toString();
                            // 获取变量的行数
                            Range expressionRange = argument.getRange().orElse(null);
                            int variableStartLine = 0;
                            if (expressionRange != null) {
                                variableStartLine = expressionRange.begin.line;
                            }
                            // 获取变量的类名
                            String className = "";
                            Optional<ClassOrInterfaceDeclaration> classOrInterface = argument.findAncestor(ClassOrInterfaceDeclaration.class);
                            if (classOrInterface.isPresent()) {
                                // 获取类名
                                className = classOrInterface.get().getNameAsString();
                            }
                            ParameterInfo paraInfo = new ParameterInfo(variableName, className, variableStartLine);
                            // 把 parameterInfo 传入参数列表
                            parameterInfoList.add(paraInfo);
                        }

                        this.invokedParameters.add(parameterInfoList);
                    }
                } catch (Exception e) {
                    System.out.println(e);
                    System.out.println("无法解析方法调用: " + methodCall);
                }
            }
        }
    }
}
