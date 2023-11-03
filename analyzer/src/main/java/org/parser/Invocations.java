package org.parser;

import com.github.javaparser.ast.body.MethodDeclaration;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class Invocations {
    private Map<String, Methods> table;
    public Invocations() {
        table = new HashMap<>();
    }
    public void addNode(String packageName, String className, MethodDeclaration declaration) {
        Methods node = new Methods(packageName, className, declaration);
        if (!table.containsKey(node.getIdentifier())) {
            table.put(node.getIdentifier(), node);
        }
    }
    public void addNode(String packageName, String className, String methodName) {
        Methods node = new Methods(packageName, className, methodName);
        if (!table.containsKey(node.getIdentifier())) {
            table.put(node.getIdentifier(), node);
        }
    }

    public void addEdge(Methods caller, Methods callee) {
        caller.addCallee(callee);
        callee.addCaller(caller);
    }
    public void addEdge(String caller, String callee) {
        addEdge(findMethod(caller), findMethod(callee));
    }
    public Methods findMethod(String identifier) {
        if (table.containsKey(identifier)) {
            return table.get(identifier);
        }
        return null;
    }
    public ArrayList<Methods> getAllMethods() {
        return new ArrayList<>(table.values());
    }
}
