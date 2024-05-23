package pt.up.fe.comp2024.optimization.REGopt;

import java.util.HashSet;
import java.util.Set;

public class REGNode {
    private final String name;
    private Set<REGNode> edges = new HashSet<>();
    private int color;

    public REGNode(String name) {
        this.name = name;
        this.color = -1;
    }

    public String getName() {
        return name;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void addEdge(REGNode node) {
        edges.add(node);
    }

    public Set<REGNode> getEdges() {
        return edges;
    }
}
