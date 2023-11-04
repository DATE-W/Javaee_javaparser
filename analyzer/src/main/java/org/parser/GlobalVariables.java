package org.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.File;
import java.util.Scanner;

//单例模式
public class GlobalVariables {
    private static GlobalVariables instance;
    private static String projectPath;
    public static com.github.javaparser.JavaParser CONFIGURED_PARSER;
    private static Scanner scanner;

    //将构造函数声明为私有的，是为了不让外界调用构造函数
    private GlobalVariables() {
        // nothing
    }

    public static String getProjectPath() {
        if (projectPath == null) {
            projectPath = "src/main/java/";
        }
        return projectPath;
    }

    public static com.github.javaparser.JavaParser getJavaParser() {
        if (CONFIGURED_PARSER == null) {
            CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
            combinedTypeSolver.add(new ReflectionTypeSolver());
            combinedTypeSolver.add(new JavaParserTypeSolver(new File(getProjectPath())));
            JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);

            ParserConfiguration parserConfig = new ParserConfiguration();
            parserConfig.setSymbolResolver(symbolSolver);

            CONFIGURED_PARSER = new JavaParser(parserConfig);
        }
        return CONFIGURED_PARSER;
    }

    public static Scanner getScanner()
    {
        if(scanner==null) {
            scanner = new Scanner(System.in);
        }
        return scanner;
    }
}
