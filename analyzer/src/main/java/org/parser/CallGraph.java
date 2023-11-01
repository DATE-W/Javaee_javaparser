package org.parser;

import java.util.ArrayList;
import java.util.List;

public class CallGraph {
    private List<CallNode> nodes;

    public CallGraph() {
        this.nodes = new ArrayList<>();
    }

    public List<CallNode> getNodes() {
        return nodes;
    }

    public void addNode(CallNode node) {
        nodes.add(node);
    }

    // 添加方法来添加有向边
    public void addDirectedEdge(CallNode source, CallNode destination) {
        source.addNextNode(destination);
    }
}

