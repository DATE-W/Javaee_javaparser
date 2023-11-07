package org.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class ParsersAdapter {
    public static JavaParser CONFIGURED_PARSER; // 配置好的 JavaParser 对象
    public static JavaParserFacade FACADE;
    private static HashMap<String, ResolvedReferenceTypeDeclaration> referenceTypeMap;
    private String path; // 项目路径
    private int classesCount;

    // 构造函数
    public ParsersAdapter(String path) {
        configure(path); // 配置 CONFIGURED_PARSER
        this.path = path; // 设置项目路径
        this.classesCount = 0; // 记录类的数量
    }

    // 配置 JavaParser
    public void configure(String path) {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        combinedTypeSolver.add(new JavaParserTypeSolver(new File(path)));
        // 用类型解析器创建一个JavaParserFacade实例，它提供解析的高级接口
        FACADE = JavaParserFacade.get(combinedTypeSolver);

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        ParserConfiguration parserConfig = new ParserConfiguration();
        parserConfig.setSymbolResolver(symbolSolver);
        CONFIGURED_PARSER = new JavaParser(parserConfig);
    }

    // 分析项目目录下的所有 java 文件
    public void analyzeAllFiles(Invocations invocations) {
        File directory = new File(path + "/main"); // 项目目录
        Queue<File> queue = new LinkedList<>(); // 基于链表的队列

        // 将所有方法加入图中
        queue.add(directory); // 根结点入队
        while (!queue.isEmpty()) {
            File target = queue.remove(); // 取队头
            if (target.isFile()) { // 文件为叶结点
                if (target.getName().endsWith(".java")) {
                    addAllMethodsFromFile(target, invocations); // 分析文件
                }
                continue;
            }
            for (File son : target.listFiles()) {
                queue.add(son); // 子结点入队
            }
        }

        // 将所有调用关系加入图中
        queue.add(directory); // 根结点入队
        while (!queue.isEmpty()) {
            File target = queue.remove(); // 取队头
            if (target.isFile()) { // 文件为叶结点
                if (target.getName().endsWith(".java")) {
                    parseFile(target, invocations); // 分析文件
                }
                continue;
            }
            for (File son : target.listFiles()) {
                queue.add(son); // 子结点入队
            }
        }
    }

    // 将文件中的所有方法加入图中
    public void addAllMethodsFromFile(File file, Invocations invocations) {
        JavaParser javaParser = CONFIGURED_PARSER; // 使用配置好的 parser
        try {
            CompilationUnit cu = javaParser.parse(file).getResult().orElse(null);
            if (cu == null) {
                return;
            }
            String packageName = cu.getPackageDeclaration().get().getNameAsString(); // 获取包名
            for (ClassOrInterfaceDeclaration classDeclaration : cu.findAll(ClassOrInterfaceDeclaration.class)) { // 遍历所有类
                String className = classDeclaration.getNameAsString(); // 获取类名
                classesCount++;
                for (MethodDeclaration methodDeclaration : classDeclaration.getMethods()) { // 遍历所有方法
                    invocations.addNode(packageName, className, methodDeclaration); // 添加方法结点
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void recordClassInstantiation(CompilationUnit cu)
    {
        // 收集所有的对象创建表达式（例如 new SomeClass()），以确定变量实际引用的类型
        cu.findAll(ObjectCreationExpr.class).forEach(objCreation -> {
            // 获取对象创建表达式的类型
            ResolvedType resolvedType = FACADE.convertToUsage(objCreation.getType(), objCreation);
            // 如果类型是引用类型，获取其声明
            if (resolvedType.isReferenceType()) {
                ResolvedReferenceTypeDeclaration typeDeclaration = resolvedType.asReferenceType().getTypeDeclaration().get();
                // 尝试找到变量声明节点的祖先节点
                objCreation.findAncestor(VariableDeclarator.class).ifPresent(varDecl -> {
                    // 将变量名和类型声明放入映射中
                    referenceTypeMap.put(varDecl.getNameAsString(), typeDeclaration);
                });
            }
        });
    }

    private String resolvePolymorphicFunctionCalls(MethodCallExpr methodCall) {
        // 获取方法调用的作用域，如变量名或者this
        Optional<Expression> optScope = methodCall.getScope();
        if (optScope.isPresent()) {
            // 如果作用域是变量名
            Expression scope = optScope.get();
            if (scope instanceof NameExpr) {
                // 获取变量名
                String name = ((NameExpr) scope).getNameAsString();
                // 从映射中获取变量对应的实际类型
                ResolvedReferenceTypeDeclaration actualType = referenceTypeMap.get(name);
                // 如果能找到实际类型
                if (actualType != null) {
                    try {
                        // 解析方法调用的声明
                        ResolvedMethodDeclaration method = FACADE.solve(methodCall).getCorrespondingDeclaration();
                        // 在实际类型中查找对应的方法
                        Optional<ResolvedMethodDeclaration> actualMethod = actualType.getDeclaredMethods().stream()
                                .filter(m -> m.getName().equals(method.getName()))
                                .findFirst();

                        // 如果找到，返回方法调用和声明的详细信息
                        if (actualMethod.isPresent()) {
                            return actualMethod.get().getQualifiedSignature();
                        } else {
                            return method.getQualifiedSignature();
                        }
                    } catch (Exception e) {
                        // 解析出现异常，返回错误信息
                        System.out.println("Resolution error: " + e.getMessage());
                        return "Resolution error: " + e.getMessage();
                    }
                }
            }
        }
        // 如果作用域不存在或其他任何情况，需要返回一个默认值或者错误
        return "Unable to resolve the method call.";
    }

    // 分析文件中的所有调用语句
    public void parseFile(File file, Invocations invocations) {
        JavaParser javaParser = CONFIGURED_PARSER; // 使用配置好的 parser
        try {
            CompilationUnit cu = javaParser.parse(file).getResult().orElse(null);
            if (cu == null) {
                return;
            }
            String packageName = cu.getPackageDeclaration().get().getNameAsString(); // 获取包名
            for (ClassOrInterfaceDeclaration classDeclaration : cu.findAll(ClassOrInterfaceDeclaration.class)) { // 遍历类
                String className = classDeclaration.getNameAsString(); // 获取类名
                for (MethodDeclaration methodDeclaration : classDeclaration.getMethods()) { // 遍历方法
                    Methods caller = invocations.findMethod(Methods.constructIdentifier(packageName, className, methodDeclaration.getNameAsString(), methodDeclaration.getParameters()));
                    for (MethodCallExpr methodCall : methodDeclaration.findAll(MethodCallExpr.class)) {
                        try{
                            String[] calleeInfo = methodCall.resolve().getQualifiedSignature().split("[()]"); // 被调用方法信息
                            String calleeName = methodCall.getNameAsString();
                            if (calleeInfo.length != 2) {
                                continue;
                            }
                            String[] calleeBody = calleeInfo[0].split("\\.");
                            String[] calleeParamsType = calleeInfo[1].split(", ");

                            // 获取被调用者的类和包

                            /* 注意：这里在完成 F2 之后需要继续完善 */
                            String calleeClass = null;
                            String calleePackage = null;
                            for (int i = calleeBody.length - 1; i >= 2; i--) {
                                if (calleeBody[i].contains(calleeName)) {
                                    calleeClass = calleeBody[i - 1];
                                    calleePackage = calleeBody[i - 2];
                                    break;
                                }
                            }
                            if (calleePackage == null || calleeClass == null) {
                                continue;
                            }

                            // 构造参数类型列表
                            ArrayList<String> calleeParamsList = new ArrayList<>();
                            for (String paramType : calleeParamsType) {
                                if (paramType.contains(".")) {
                                    calleeParamsList.add(paramType.substring(paramType.lastIndexOf('.') + 1));
                                } else {
                                    calleeParamsList.add(paramType);
                                }
                            }

                            invocations.addNode(calleePackage, calleeClass, calleeName,calleeParamsList); // 若不存在则加入图中
                            Methods callee = invocations.findMethod(Methods.constructIdentifier(calleePackage, calleeClass, calleeName, calleeParamsList)); // 找到被调用函数
                            invocations.addEdge(caller, callee); // 建立关系
                        } catch (Exception e) {
                            System.out.println(e);
                            System.out.println("无法解析方法调用: " + methodCall);
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public int countClasses() {
        return classesCount;
    }
}
