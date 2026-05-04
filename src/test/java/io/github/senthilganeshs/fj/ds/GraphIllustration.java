package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.typeclass.*;

/**
 * A realistic example of using the Graph data structure for Task Dependency Management.
 * This demonstrates building a dependency graph, performing a topological sort 
 * to find a valid execution order, and using BFS to find reachable tasks.
 */
public class GraphIllustration {

    public static void main(String[] args) {
        System.out.println("--- Task Dependency Management Example ---");

        // 1. Create a dependency graph for a build system
        // Edges mean: "Task A must be completed before Task B"
        Graph<String> buildGraph = Graph.<String>nil()
            .addEdge("Clean", "Compile")
            .addEdge("Compile", "Test")
            .addEdge("Compile", "Package")
            .addEdge("Test", "Deploy")
            .addEdge("Package", "Deploy")
            .addEdge("Deploy", "Notify");

        System.out.println("Full Dependency Graph: " + buildGraph);

        // 2. Find a valid execution order using Topological Sort
        System.out.println("\n--- 1. Topological Sort (Execution Order) ---");
        String result = ((Maybe<String>) buildGraph.topologicalSort().map(order -> {
            System.out.println("Valid Build Order: " + order.mkString(" -> "));
            return "SUCCESS";
        })).orElse(null);
        
        if (result == null) {
            System.out.println("[ERROR] Circular dependency detected! Cannot find a valid execution order.");
        }

        // 3. Find all tasks affected by a change in "Compile" (BFS)
        System.out.println("\n--- 2. Impact Analysis (BFS) ---");
        List<String> affectedTasks = List.from(buildGraph.bfs("Compile"));
        System.out.println("Tasks affected if 'Compile' changes: " + affectedTasks.mkString(", "));

        // 4. Check successors of a specific task
        System.out.println("\n--- 3. Immediate Dependencies ---");
        System.out.println("Tasks directly depending on 'Compile': " + buildGraph.successors("Compile"));

        // 5. Demonstrate cycle detection
        System.out.println("\n--- 4. Cycle Detection ---");
        Graph<String> cyclicGraph = buildGraph.addEdge("Notify", "Clean");
        String cycleResult = ((Maybe<String>) cyclicGraph.topologicalSort().map(order -> {
            System.out.println("[ERROR] Failed to detect cycle! Order: " + order);
            return "FAILURE";
        })).orElse("SUCCESS");

        if (cycleResult.equals("SUCCESS")) {
            System.out.println("[SUCCESS] Cycle detected between 'Notify' and 'Clean'. Correctly refused to sort.");
        }
    }
}
