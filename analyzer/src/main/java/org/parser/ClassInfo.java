package org.parser;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClassInfo implements Analyzable<MethodInfo> {

    private final CompilationUnit unit;
    private final List<MethodInfo> methods;

    public ClassInfo(CompilationUnit unit) {
        this.unit = unit;
        this.methods = new ArrayList<>();
    }

    public List<MethodInfo> analyze() {
        // 获取CompilationUnit中所有的类和接口声明
        List<ClassOrInterfaceDeclaration> classDeclarations = unit.findAll(ClassOrInterfaceDeclaration.class);

        for (ClassOrInterfaceDeclaration classDeclaration : classDeclarations) {
            // 获取类或接口中所有的方法声明
            List<MethodDeclaration> methodDeclarations = classDeclaration.getMethods();
            for (MethodDeclaration methodDeclaration : methodDeclarations) {
                // 为每个方法创建MethodInfo对象，并添加到methods列表中
                MethodInfo methodInfo = new MethodInfo(methodDeclaration);
                methods.add(methodInfo);
            }
        }

        return methods;
    }

    public String getClassName() {
        // 获取CompilationUnit中的第一个类或接口声明的名称
        return unit.getClassByName(unit.getType(0).getNameAsString()).orElse(null).getNameAsString();
    }

    public String getPackageName() {
        // 获取包声明
        Optional<PackageDeclaration> packageDeclaration = unit.getPackageDeclaration();

        // 如果包声明存在，则返回包名，否则返回一个空字符串或其他适当的默认值
        return packageDeclaration.map(pd -> pd.getName().asString()).orElse("");
    }

    public List<MethodInfo> getMethods() {
        return methods;
    }
}
