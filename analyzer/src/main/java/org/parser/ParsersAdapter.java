package org.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import static com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver.getJarTypeSolver;

public class ParsersAdapter {
    public static JavaParser CONFIGURED_PARSER; // 配置好的 JavaParser 对象
    private String path; // 项目路径
    private int classesCount; // 类的个数统计
    public static TypeSolver TYPE_SOLVER;

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

        // 递归查找指定目录下的所有 .jar 文件并添加到类型解析器中
        File m2Repository = new File("C:\\Users\\85025\\.m2\\repository\\com\\github\\javaparser");
        List<File> jarFiles = findJarFiles(m2Repository);
        for (File jarFile : jarFiles) {
            try {
                combinedTypeSolver.add(getJarTypeSolver(jarFile.getAbsolutePath()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        TYPE_SOLVER=combinedTypeSolver;
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        ParserConfiguration parserConfig = new ParserConfiguration();
        parserConfig.setSymbolResolver(symbolSolver);
        CONFIGURED_PARSER = new JavaParser(parserConfig);
    }

    private static List<File> findJarFiles(File directory) {
        List<File> jarFiles = new ArrayList<>();
        if (directory != null && directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        // 如果是目录，递归查找
                        jarFiles.addAll(findJarFiles(file));
                    } else if (file.isFile() && file.getName().endsWith(".jar")) {
                        // 如果是 .jar 文件，添加到列表中
                        jarFiles.add(file);
                    }
                }
            }
        }
        return jarFiles;
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
                    invocations.addNode(packageName + "." + className, methodDeclaration); // 添加方法结点
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
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
                    Methods caller = invocations.findMethod(Methods.constructIdentifier(packageName + "." + className, methodDeclaration.getNameAsString(), methodDeclaration.getParameters()));
                    for (MethodCallExpr methodCall : methodDeclaration.findAll(MethodCallExpr.class)) {
                        try {
                            String prefix = resolvePolymophicInvoke(methodCall);
                            String[] calleeInfo = methodCall.resolve().getQualifiedSignature().split("[()]"); // 被调用方法信息
                            String calleeName = methodCall.getNameAsString();
                            String calleePrefix = calleeInfo[0].substring(0, calleeInfo[0].lastIndexOf('.'));
                            if (prefix != null) {
                                calleePrefix = prefix;
                            }

                            // 构造参数类型列表
                            ArrayList<String> calleeParamsList = new ArrayList<>();
                            if (calleeInfo.length >= 2) {
                                String[] calleeParamsType = calleeInfo[1].split(", ");
                                for (String paramType : calleeParamsType) {
                                    if (paramType.contains(".")) {
                                        calleeParamsList.add(paramType.substring(paramType.lastIndexOf('.') + 1));
                                    } else {
                                        calleeParamsList.add(paramType);
                                    }
                                }
                            }

                            // bug: 不能解决参数类型为 Container<String> 的情况

                            invocations.addNode(calleePrefix, calleeName,calleeParamsList); // 若不存在则加入图中
                            Methods callee = invocations.findMethod(Methods.constructIdentifier(calleePrefix, calleeName, calleeParamsList)); // 找到被调用函数
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

    // 返回统计的类个数
    public int countClasses() {
        return classesCount;
    }

    // 判断多态
    public String resolvePolymophicInvoke(MethodCallExpr methodCall){
        try {
            Optional<Expression> scope = methodCall.getScope();
            if (scope.isPresent() && scope.get() instanceof NameExpr) {
                NameExpr nameExpr = (NameExpr) scope.get();
                String name = nameExpr.getNameAsString();

                // 初始化resolvedType
                ResolvedType resolvedType = null;

                // 找到最近的声明
                Node currentNode = methodCall;
                Optional<VariableDeclarator> variableDeclarator = Optional.empty();

                while (!(currentNode instanceof CompilationUnit)) {
                    if (currentNode instanceof BlockStmt) {
                        BlockStmt blockStmt = (BlockStmt) currentNode;
                        variableDeclarator = blockStmt.getStatements().stream()
                                .filter(Statement::isExpressionStmt)
                                .map(Statement::asExpressionStmt)
                                .map(ExpressionStmt::getExpression)
                                .filter(Expression::isVariableDeclarationExpr)
                                .map(Expression::asVariableDeclarationExpr)
                                .flatMap(vde -> vde.getVariables().stream())
                                .filter(vd -> vd.getNameAsString().equals(name))
                                .findFirst();

                        if (variableDeclarator.isPresent()) {
                            break;
                        }
                    }
                    else if (currentNode instanceof SwitchStmt) {
                        SwitchStmt switchStmt = (SwitchStmt) currentNode;
                        for (SwitchEntry switchEntry : switchStmt.getEntries()) {
                            for (Statement entryStmt : switchEntry.getStatements()) {
                                // Now check if there's a variable declaration within the switch entry
                                if (entryStmt.isExpressionStmt() && entryStmt.asExpressionStmt().getExpression().isVariableDeclarationExpr()) {
                                    VariableDeclarationExpr varDeclExpr = entryStmt.asExpressionStmt().getExpression().asVariableDeclarationExpr();
                                    Optional<VariableDeclarator> optVarDecl = varDeclExpr.getVariables().stream()
                                            .filter(vd -> vd.getNameAsString().equals(name))
                                            .findFirst();

                                    if (optVarDecl.isPresent()) {
                                        variableDeclarator = optVarDecl;
                                        break; // Break from the switchEntry loop
                                    }
                                }
                            }
                            if (variableDeclarator.isPresent()) {
                                break; // Break from the switchStmt loop if we've found our variable
                            }
                        }
                        if (variableDeclarator.isPresent()) {
                            break; // Break from the switchStmt loop if we've found our variable
                        }
                    }
                    // Check if we've reached an ObjectCreationExpr and if it contains the scope
                    else if (currentNode instanceof ObjectCreationExpr) {
                        ObjectCreationExpr objectCreationExpr = (ObjectCreationExpr) currentNode;
                        boolean scopeIsInObjectCreation = objectCreationExpr.getArguments().stream()
                                .anyMatch(arg -> arg.toString().equals(name));
                        if (scopeIsInObjectCreation) {
                            break; // We found the scope in an object creation
                        }
                    }
                    currentNode = currentNode.getParentNode().orElse(null);
                }

                // Attempt to resolve the type if we've found the declaration
                if (variableDeclarator.isPresent()) {
                    VariableDeclarator varDecl = variableDeclarator.get();
                    try {
                        // Check the initializer of the variable to determine the actual type
                        if (varDecl.getInitializer().isPresent() && varDecl.getInitializer().get().isObjectCreationExpr()) {
                            // If the variable is initialized using 'new', resolve the type of the created object
                            ObjectCreationExpr creationExpr = varDecl.getInitializer().get().asObjectCreationExpr();
                            resolvedType = JavaParserFacade.get(TYPE_SOLVER).convertToUsage(creationExpr.getType(), methodCall);
                        } else {
                            // Otherwise, use the declared type
                            resolvedType = JavaParserFacade.get(TYPE_SOLVER).getType(nameExpr);
                        }
                        // Output the qualified name of the type to see where the 'speak' method comes from
                        return resolvedType.asReferenceType().getQualifiedName();
                    } catch (UnsolvedSymbolException e) {
                        System.err.println("Unresolved type for method call: " + methodCall);
                    } catch (RuntimeException e) {
                        System.err.println("Error resolving type for method call: " + methodCall + " - " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            // 主方法中的异常处理，打印堆栈跟踪
            e.printStackTrace();
        }
        return null;
    }
}
