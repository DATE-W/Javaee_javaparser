package org.parser;

import java.util.List;
import java.util.ArrayList;


public class CallNode {
    private String name;
    private String parameterClass;
    private int parameterLine;

    private int scopeStartLine;

    private int scopeEndLine;
    private int scopeType;

    public List<CallNode> nextNodes; // 用于表示下一步连接的节点列表
    public List<CallNode> prevNodes; // 用于表示下一步连接的节点列表

    public CallNode(String name, String parameterClass, int parameterLine) {
        this.name = name;
        this.parameterClass = parameterClass;
        this.parameterLine = parameterLine;
    }

    // 添加方法来添加下一步的连接，也就是这个Node会去往何处
    public void addNextNode(CallNode nextNode) {
        if (nextNodes == null) {
            nextNodes = new ArrayList<>();
            if (!nextNodes.contains(nextNode)) {
                this.nextNodes.add(nextNode);   // 这个的下一个是 nextNode
            }
        }
    }


    public String getNodeInfo() {
        return name + " in " + parameterClass + ":" + Integer.toString(parameterLine);
    }

}
