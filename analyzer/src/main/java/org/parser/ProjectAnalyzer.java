package org.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.Type;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import com.github.javaparser.Range;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.common.collect.HashMultiset;
import org.checkerframework.checker.units.qual.A;


public class ProjectAnalyzer implements Analyzable<ClassInfoInFile> {
    // 存储项目中的Java文件列表
    private final List<File> javaFiles;
    // 项目的包名
    private String packageName;


    //JieChu: 严文昊说CallGraph结构没有用了，所以我就把它注释掉了
//    public CallGraph graph = new CallGraph();

    //JieChu: 我觉得拿classInfos做成员变量很合理
    public List<ClassInfoInFile> classInfos=new ArrayList<>();     // 调用 analyze 获取所有类的信息
    //JieChu: 我觉得拿methodInfos做成员变量很合理
    public List<MethodInfo> methodInfos = new ArrayList<>();

    // 构造函数，初始化分析器并配置JavaParser
    public ProjectAnalyzer(String packageName) {
        this.packageName = packageName;
        // 获取包目录下的所有Java文件
        File directory = new File(GlobalVariables.getProjectPath() + packageName);
        this.javaFiles = getJavaFiles(directory);

        //JieChu: 为classInfos和methodInfos两大成员变量初始化
        analyze();
    }

    // 递归获取目录下的所有Java文件
    public static List<File> getJavaFiles(File directory) {
        List<File> javaFiles = new ArrayList<>();
        File[] files = directory.listFiles(); // 获取目录中的所有文件和子目录

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // 如果是一个目录，则递归遍历
                    javaFiles.addAll(getJavaFiles(file));
                } else if (file.getName().endsWith(".java")) {
                    // 如果是一个Java文件，则添加到列表中
                    javaFiles.add(file);
                }
            }
        }

        return javaFiles;
    }

    // 分析项目中的类，并返回类信息列表
    public List<ClassInfoInFile> analyze() {
        JavaParser javaParser = GlobalVariables.getJavaParser();

        for (File javaFile : javaFiles) {
            try {
                // 解析Java文件，一个cu代表一个java文件
                CompilationUnit cu = javaParser.parse(javaFile).getResult().orElse(null);
                if (cu != null) {
                    // 创建类信息对象并分析
                    ClassInfoInFile classInfoInFile = new ClassInfoInFile(cu);
                    /*
                    JieChu: 事实上，ClassInfoInFile.analyze()方法是根据传进去的cu来分析一个类中所有的MethodInfo,
                    并将这些MethodInfo存入ClassInfo.methods。
                    事实上，ClassInfoInFile的主要作用就是储存一个类的所有MethodInfo
                     */
                    classInfoInFile.analyze();
                    classInfos.add(classInfoInFile);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        /*
        下面的3个for循环初始化被解析项目中所有method的信息
         */
        //classInfos获取了所有类的信息(类中包含的所有方法)，将其添加到methodInfos中
        for (ClassInfoInFile classInfo : classInfos) {
            methodInfos.addAll(classInfo.getMethods());     // 把 classInfos 里的信息加入 methodInfos，把一个列表中的元素加到另一个列表中
        }
        //调用methodInfo.analyze让每个methodInfos中的方法知道自己被谁调用了+调用了谁
        for (MethodInfo methodInfo : methodInfos) {
            methodInfo.analyze(methodInfos);
        }
        //MethodInfo的getInvokedParameters方法是用来得到当该MethodInfo被调用时的所有参数来源(参数名+所属类+)的
        //MethodInfo.getInvokedParameters要发挥作用必须得到MethodInfo.methodCallingThis字段被完全初始化，也就是上面的methodInfo.analyze(methodInfos);被调用
        for (MethodInfo methodInfo : methodInfos) {
            methodInfo.getInvokedParameters();
        }

        return classInfos;
    }



    public Map.Entry<Boolean, List<MethodInfo>> checkFunctionOverload(String methodName, String className)
    {
        int reloadFunctionsNum=0;
        List<MethodInfo> reloadMethods=new ArrayList<>();
        for (MethodInfo methodInfo : methodInfos){
            if (methodInfo.getMethodName().equals(methodName) &&
                    methodInfo.getClassName().equals(className)){
                reloadFunctionsNum++;
                reloadMethods.add(methodInfo);
            }
        }

        if(reloadFunctionsNum==0){
            return null;
        }
        else if(reloadFunctionsNum==1){
            return  new AbstractMap.SimpleEntry<>(false, reloadMethods);
        }
        else{
            return new AbstractMap.SimpleEntry<>(true, reloadMethods);
        }
    }


    // 分析指定类中的特定方法，并显示其调用的方法和被哪些方法调用
    //假如传入analyzeSpecificMethod的参数chosenReloadMethodParams是null，则说明没有方法重载
    public void analyzeSpecificMethod(String methodName, String className, int depth, Map<String,Type> chosenReloadMethodParams) {
        for (MethodInfo methodInfo : methodInfos) {
            if (methodInfo.getMethodName().equals(methodName) && methodInfo.getClassName().equals(className)){
                //chosenReloadMethodParams!=null，说明有方法重载。
                //有方法重载，则判断目前遍历到的方法的参数列表是否就是要找的方法，若不是，则continue
                if(chosenReloadMethodParams!=null &&
                        !HashMultiset.create(methodInfo.getParamList().values()).equals(HashMultiset.create(chosenReloadMethodParams.values()))){
                    continue;
                }
                System.out.println("Input:");
                System.out.println(methodInfo.getMethodName() + ", " + className + ", depth=" + depth);
                System.out.println("========");
                System.out.println("Output:");
                String tempInvoked = methodInfo.getInvokedBy(depth, 1, packageName);
                String tempInvokes = methodInfo.getInvokes(depth, 1, packageName);
                if (!tempInvoked.equals("")) {
                    System.out.println("It is invoked by the following:\n" + tempInvoked);
                } else {
                    System.out.println("It is invoked by the following:\n[NONE]");
                }
                if (!tempInvokes.equals("")) {
                    System.out.println("It invokes the following:\n" + tempInvokes);
                } else {
                    System.out.println("It invokes the following:\n[NONE]");
                }
                //找到一个匹配的方法肯定就退出了啊，为了性能。
                break;
            }
        }
    }
    // 查看被调用的方法


    public void findAllUsedExpr() {
        JavaParser javaParser = GlobalVariables.getJavaParser();;

        for (File javaFile : javaFiles) {
            try {
                // Parse the Java file.
                CompilationUnit cu = javaParser.parse(javaFile).getResult().orElse(null);
                if (cu != null) {
                    // 对所有的类遍历做处理，也即这个处理是类级别的；
                    // javaparser 没有一个获取变量 scope 的功能，这意味这变量的 scope 需要我们自己去找？ 比如在 if 语句中定义变量
                    // 更正：javaparser 可以通过获取 parent 的类型得到变量的作用域是函数、类、还是条件判断后面的语句 也即AfterConditionalStatement
                    // 我们是否还需要一个和 scope 有关的数据结构，便于更精确的判断
                    List<ClassOrInterfaceDeclaration> classDeclarations = cu.findAll(ClassOrInterfaceDeclaration.class);
                    for (ClassOrInterfaceDeclaration classDeclaration : classDeclarations) {
                        // 第一种流动情况：声明
//                        processDeclarations(classDeclaration);
                        // 第二种流动情况：赋值
                        // 赋值的普通情况：右边是普通表达式
//                        processAssignments(classDeclaration);
                        // 赋值的特殊情况：右值是函数的返回值
                        // 那么这里就要进入函数的 return 语句，继续往上分析
                        // 这里还没做....(请杰哥做)
                        // JieChu: Done.
//                        processAssignmentsWithMethodCalls(classDeclaration);
                        // 第三种流动情况：函数调用（仅考虑参数调用）
//                        processMethodCalls(classDeclaration);
                        // 第四种流动情况：函数调用返回值
                        // 其它边界情况
                        // 如果有 if else 语句？ 如果有 for 语句？ 如果先name1 = name2，再name2 = name3 ，这不应该允许它汇聚！
                        // 这意味着我们需要在 callNode 里增加信息，表达它是哪个方法里的，如果它是类变量，那么就是哪个类里的
                        // 当前没有办法处理类成员变量
                        // 另一个注意点！！！！！
                        // 当前，我们对于声明和赋值的右值，都只考虑了右值是一个普通变量(nameExpr)的情形
                        // 但是，如果是 BinaryExpr，也就是类似于 name1 + name2 + "hello" 如何去做处理呢？
                        // 首先要从 name1 + name2 + "hello" 来提取变量 -> 这个可以解决 name2 + "hello" 这种情况了
                        // 可是，如果出现 name1 + name2 图里就会出现汇聚的情况了！这意味着我们回头追溯的时候需要分叉去追溯
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }


//    private void processDeclarations(ClassOrInterfaceDeclaration classDeclaration) {
//        // 找到所有的变量声明语句
//        List<VariableDeclarator> declarators = classDeclaration.findAll(VariableDeclarator.class);
//        for (VariableDeclarator declarator : declarators) {
//            // 获取声明的变量名
//            String variableName = declarator.getNameAsString();
//            // 获取声明变量的初始化表达式（如果有）
//            Expression initializationExpr = declarator.getInitializer().orElse(null);
//            // 获取声明语句的源代码范围
//            Range declarationRange = declarator.getRange().orElse(null);
//            // 获取起始行
//            int startLine = (declarationRange != null) ? declarationRange.begin.line : 0;
//
//            // 创建 CallNode，将变量名作为节点信息，即：被声明的变量名+在哪个类里声明了这个变量+声明这个变量在该文件的哪一行
//            CallNode nextNode = new CallNode(variableName, classDeclaration.getNameAsString(), startLine);
//            // 如果有初始化表达式，将其作为 nextNode 添加到 CallNode
//            if(initializationExpr instanceof MethodCallExpr)
//            {
//                // 右边是方法调用，处理这个情况
//                MethodCallExpr methodCall = (MethodCallExpr) initializationExpr;
//                Optional<MethodDeclaration> methodDeclaration = findMethodDeclaration(classDeclaration, methodCall);
//
//                if (methodDeclaration.isPresent()) {
//                    // 找到方法声明，现在寻找return语句
//                    methodDeclaration.get().findAll(ReturnStmt.class).forEach(returnStmt -> {
//                        Expression returnExpr = returnStmt.getExpression().orElse(null);
//                        // Return语句返回了一个变量
//                        if (returnExpr instanceof NameExpr) {
//                            String returnVariableName = ((NameExpr) returnExpr).getNameAsString();
//
//                            CallNode callNode = new CallNode(returnVariableName, classDeclaration.getNameAsString(), startLine);
////                            CallNode nextNode = new CallNode(variableName, classDeclaration.getNameAsString(), startLine);
//                            callNode.addNextNode(nextNode);
//                            // 将 CallNode 添加到 CallGraph
//                            graph.addNode(callNode);
//
//                            System.out.println(callNode.getNodeInfo() + " -> " + nextNode.getNodeInfo());
//                        }
//                    });
//                }
//            }
//            else if (initializationExpr != null) {
//                CallNode callNode = new CallNode(initializationExpr.toString(), classDeclaration.getNameAsString(), startLine);
//                callNode.addNextNode(nextNode);
//                // 将 CallNode 添加到 CallGraph
//                graph.addNode(callNode);
//                System.out.println(callNode.getNodeInfo() + " -> " + nextNode.getNodeInfo());
//            }
//        }
//    }
//
//
//    private void processAssignments(ClassOrInterfaceDeclaration classDeclaration) {
//        // 处理赋值情况的代码
//        List<AssignExpr> assignExprs = classDeclaration.findAll(AssignExpr.class);
//        for (AssignExpr assignExpr : assignExprs) {
//            // 获取赋值前面的内容
//            Expression leftExpr = assignExpr.getTarget();
//            // 获取赋值后面的内容
//            Expression rightExpr = assignExpr.getValue();
//            // 获取赋值表达式的源代码范围
//            Range expressionRange = assignExpr.getRange().orElse(null);
//            // 获取起始行
//            int startLine = 0;
//            if (expressionRange != null)
//                startLine = expressionRange.begin.line;
//
//            // 创建 CallNode，并将赋值前面的内容添加到 CallNode。节点包括：右值的名称/描述+在哪个类里做了赋值+在哪一行做了赋值
//            CallNode callNode = new CallNode(rightExpr.toString().split("=")[0], classDeclaration.getNameAsString(), startLine);
//
//            // 如果赋值后面的内容不为空，将其作为 nextNode 添加到 CallNode
//            if (rightExpr instanceof MethodCallExpr)
//            {
//
//            }
//            else if (rightExpr != null) {
//                CallNode nextNode = new CallNode(leftExpr.toString(), classDeclaration.getNameAsString(), startLine);
//                callNode.addNextNode(nextNode);
//                // 将 CallNode 添加到 CallGraph
//                graph.addNode(callNode);
//                System.out.println(callNode.getNodeInfo() + " -> " + nextNode.getNodeInfo());
//            }
//        }
//    }
//
//    //JieChu: 这个processAssignmentsWithMethodCalls目前只能处理等号右边的函数只返回一个变量的情况，我还没考虑返回的是一个表达式
//    private void processAssignmentsWithMethodCalls(ClassOrInterfaceDeclaration classDeclaration) {
//        // 处理赋值语句，特别是右边是方法调用的情况
//        List<AssignExpr> assignExprs = classDeclaration.findAll(AssignExpr.class);
//        for (AssignExpr assignExpr : assignExprs) {
//            // 获取赋值左边和右边的内容
//            Expression leftExpr = assignExpr.getTarget();
//            Expression rightExpr = assignExpr.getValue();
//
//            // 获取赋值表达式的源代码范围和起始行
//            Range expressionRange = assignExpr.getRange().orElse(null);
//            int startLine = (expressionRange != null) ? expressionRange.begin.line : 0;
//
//            if (rightExpr instanceof MethodCallExpr) {
//                // 右边是方法调用，处理这个情况
//                MethodCallExpr methodCall = (MethodCallExpr) rightExpr;
//                Optional<MethodDeclaration> methodDeclaration = findMethodDeclaration(classDeclaration, methodCall);
//
//                if (methodDeclaration.isPresent()) {
//                    // 找到方法声明，现在寻找return语句
//                    methodDeclaration.get().findAll(ReturnStmt.class).forEach(returnStmt -> {
//                        Expression returnExpr = returnStmt.getExpression().orElse(null);
//                        // Return语句返回了一个变量
//                        if (returnExpr instanceof NameExpr) {
//                            String returnVariableName = ((NameExpr) returnExpr).getNameAsString();
//
//                            CallNode callNode = new CallNode(returnVariableName, classDeclaration.getNameAsString(), startLine);
//                            CallNode nextNode = new CallNode(leftExpr.toString(), classDeclaration.getNameAsString(), startLine);
//                            callNode.addNextNode(nextNode);
//                            // 将 CallNode 添加到 CallGraph
//                            graph.addNode(callNode);
//
//                            System.out.println(returnVariableName + " in " + classDeclaration.getNameAsString() + ":" + startLine + " -> " + leftExpr);
//                        }
//                    });
//                }
//            }
//        }
//    }
//
//    //JieChu: 我用这个findMethodDeclaration来替代
//    // classDeclaration.findFirst(MethodDeclaration.class,md -> md.getNameAsString().equals(methodName));
//    //以查到函数定义，是因为我觉得我们应该考虑本类外的函数定义，我怀疑查找类外的函数会有些复杂，所以先放个findMethodDeclaration在这儿。
//    private Optional<MethodDeclaration> findMethodDeclaration(ClassOrInterfaceDeclaration classDeclaration, MethodCallExpr methodCall) {
//        // 根据方法调用查找当前类中对应的方法声明
//        String methodName = methodCall.getNameAsString();
//        for (MethodDeclaration method : classDeclaration.getMethods()) {
//            if (method.getNameAsString().equals(methodName)) {
//                // 参数数量和方法调用的参数数量相同，可以增加更多检查确保它是正确的方法
//                if (method.getParameters().size() == methodCall.getArguments().size()) {
//                    return Optional.of(method);
//                }
//            }
//        }
//        return Optional.empty();
//    }
//
//
//    private void processMethodCalls(ClassOrInterfaceDeclaration classDeclaration) {
//        // 再去找函数调用的情况
//        List<MethodCallExpr> methodCalls = classDeclaration.findAll(MethodCallExpr.class);
//        for (MethodCallExpr methodCall : methodCalls) {
//            String methodName = methodCall.getNameAsString(); // 获取方法名
//            NodeList<Expression> arguments = methodCall.getArguments(); // 获取参数列表
//
//            // 找到函数的原始定义
//            //JieChu: 但是这里只能找到本类中的函数定义啊，假如这里的对函数传参的情况中，函数是其他类的函数呢？
//            Optional<MethodDeclaration> methodDeclaration = findMethodDeclaration(classDeclaration, methodCall);
//
//            if (methodDeclaration.isPresent()) {
//                MethodDeclaration method = methodDeclaration.get();
//                NodeList<Parameter> parameters = method.getParameters();
//                // 得到函数的形参列表
//
//                // 处理参数，将实际参数与形参关联起来
//                for (int i = 0; i < arguments.size(); i++) {
//                    String paramName = parameters.get(i).getNameAsString();
//                    String argName = arguments.get(i).toString();
//
//                    // 创建 CallNode，表示函数的形参
//                    CallNode paramNode = new CallNode(paramName, classDeclaration.getNameAsString(), method.getBegin().get().line);
//
//                    // 创建 CallNode，表示函数的实际参数
//                    CallNode argNode = new CallNode(argName, classDeclaration.getNameAsString(), methodCall.getBegin().get().line);
//                    argNode.addNextNode(paramNode);
//
//                    // 添加形参和实际参数到 CallGraph
//                    graph.addNode(paramNode);
//                    graph.addNode(argNode);
//
//                    System.out.println(argNode.getNodeInfo() + " -> " + paramNode.getNodeInfo());
//                }
//            }
//        }
//    }
}