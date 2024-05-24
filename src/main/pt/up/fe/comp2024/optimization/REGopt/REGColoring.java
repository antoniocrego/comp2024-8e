package pt.up.fe.comp2024.optimization.REGopt;

import java.util.*;

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

            var copy = new HashSet<>(neighbors);
            for (REGNode neighbor : copy) {
                neighbors.remove(neighbor);
                if (neighbors.size()<numColors) {
                    stack.push(neighbor.getName());
                }
            }
        }
    }
}
