package pt.up.fe.comp2024.optimization.REGopt;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class REGColoring {
    public REGColoring() {
    }

    public void colorGraph(REGGraph graph) {
        Stack<String> stack = new Stack<>();
        int numColors = graph.size();
        for (String node : graph.getNodes()) {
            if (graph.getEdges(node).size() < numColors)
                stack.push(node);
        }

        while (!stack.isEmpty()) {
            String node = stack.pop();
            Set<REGNode> neighbors = graph.getEdges(node);

            boolean[] usedColors = new boolean[numColors];
            for (REGNode neighbor : neighbors) {
                if (neighbor.getColor() != -1) usedColors[neighbor.getColor()] = true;
            }

            for (int i = 0; i < numColors; i++) {
                if (!usedColors[i]) {
                    graph.setNodeColor(node, i);
                    break;
                }
            }

            for (REGNode neighbor : neighbors) {
                neighbors.remove(neighbor);
                if (neighbors.size()< numColors) {
                    stack.push(neighbor.getName());
                }
            }
        }
    }
}
