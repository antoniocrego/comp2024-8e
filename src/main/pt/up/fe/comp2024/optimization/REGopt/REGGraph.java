package pt.up.fe.comp2024.optimization.REGopt;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class REGGraph {

    private final Map<String, REGNode> graph = new HashMap<>();
    private final Map<Integer, String> colors = new HashMap<>();

    public REGGraph() {

    }

    public void addNode(String name) {
        if (!graph.containsKey(name)) {
            graph.put(name, new REGNode(name));
        }
    }

    public void addEdge(String first, String second) {
        REGNode firstNode = graph.get(first);
        if (firstNode == null) {
            addNode(first);
            firstNode = graph.get(first);
        }
        REGNode secondNode = graph.get(second);
        if (secondNode == null) {
            addNode(second);
            secondNode = graph.get(second);
        }
        firstNode.addEdge(secondNode);
        secondNode.addEdge(firstNode);
    }

    public void setNodeColor(String name, int color) {
        graph.get(name).setColor(color);
        colors.put(color,name);
    }

    public int size() {
        return graph.size();
    }

    public Set<REGNode> getEdges(String name) {
        return graph.get(name).getEdges();
    }

    public Set<String> getNodes() {
        return graph.keySet();
    }

    public REGNode getNode(String name) {
        return graph.get(name);
    }

    public int colorsUsed() {
        return colors.size(); // could be that maybe there are color entries that are empty, which screws this up
    }


}
