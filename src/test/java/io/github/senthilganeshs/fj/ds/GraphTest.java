package io.github.senthilganeshs.fj.ds;

import org.testng.Assert;
import org.testng.annotations.Test;

public class GraphTest {

    @Test
    public void testGraphBasic() {
        Graph<String> g = Graph.<String>nil()
            .addEdge("A", "B")
            .addEdge("B", "C");
            
        Assert.assertEquals(g.nodes().length(), 3);
        Assert.assertTrue(g.nodes().contains("A"));
        Assert.assertTrue(g.nodes().contains("B"));
        Assert.assertTrue(g.nodes().contains("C"));
        
        Assert.assertTrue(g.successors("A").contains("B"));
        Assert.assertTrue(g.successors("B").contains("C"));
        Assert.assertTrue(g.successors("C").length() == 0);
    }

    @Test
    public void testGraphDisconnected() {
        Graph<String> g = Graph.<String>nil()
            .addEdge("A", "B")
            .addEdge("C", "D");
            
        Assert.assertEquals(g.nodes().length(), 4);
        Assert.assertEquals(g.bfs("A").length(), 2);
        Assert.assertEquals(g.bfs("C").length(), 2);
    }

    @Test
    public void testGraphBFS() {
        Graph<String> g = Graph.<String>nil()
            .addEdge("A", "B")
            .addEdge("A", "C")
            .addEdge("B", "D")
            .addEdge("C", "D");
            
        List<String> bfs = g.bfs("A");
        Assert.assertEquals(bfs.length(), 4);
        // BFS order should have A first, then B or C, then D
        Assert.assertEquals(bfs.find(i -> true).fromMaybe(""), "A");
    }

    @Test
    public void testGraphDFS() {
        Graph<String> g = Graph.<String>nil()
            .addEdge("A", "B")
            .addEdge("A", "C")
            .addEdge("B", "D")
            .addEdge("C", "D");
            
        List<String> dfs = g.dfs("A");
        Assert.assertEquals(dfs.length(), 4);
        Assert.assertEquals(dfs.find(i -> true).fromMaybe(""), "A");
    }

    @Test
    public void testGraphTopologicalSort() {
        Graph<String> g = Graph.<String>nil()
            .addEdge("A", "B")
            .addEdge("A", "C")
            .addEdge("B", "D")
            .addEdge("C", "D");
            
        Maybe<List<String>> topo = g.topologicalSort();
        Assert.assertTrue(topo.isSome());
        List<String> sorted = topo.fromMaybe(List.nil());
        Assert.assertEquals(sorted.length(), 4);
        Assert.assertEquals(sorted.find(i -> true).fromMaybe(""), "A");
    }

    @Test
    public void testGraphCyclic() {
        Graph<String> g = Graph.<String>nil()
            .addEdge("A", "B")
            .addEdge("B", "C")
            .addEdge("C", "A");
            
        Assert.assertTrue(g.topologicalSort().isNothing());
        Assert.assertEquals(g.bfs("A").length(), 3);
    }

    @Test
    public void testGraphEmptyNew() {
        Graph<String> emptyGraph = Graph.nil();
        Assert.assertEquals(emptyGraph.count(), 0);
        Assert.assertEquals(emptyGraph.length(), 0);
        Assert.assertEquals(emptyGraph.toString(), "Graph{}");
    }

    @Test
    public void testGraphSelfLoop() {
        Graph<String> g = Graph.<String>nil().addEdge("A", "A");
        Assert.assertEquals(g.nodes().length(), 1);
        Assert.assertTrue(g.topologicalSort().isNothing());
    }

    @Test
    public void testGraphTopologicalSortExhaustive() {
        // Complex DAG
        Graph<String> g = Graph.<String>nil()
            .addEdge("A", "C")
            .addEdge("B", "C")
            .addEdge("C", "D")
            .addEdge("D", "E")
            .addEdge("B", "E");
            
        Maybe<List<String>> topo = g.topologicalSort();
        Assert.assertTrue(topo.isSome());
        Assert.assertEquals(topo.fromMaybe(List.nil()).length(), 5);

        // Cyclic Graph (Deep)
        Graph<String> cyclic = g.addEdge("E", "A");
        Assert.assertTrue(cyclic.topologicalSort().isNothing());
    }

    @Test
    public void testGraphAddExistingNode() {
        Graph<String> g = Graph.<String>nil().addNode("A");
        Graph<String> g2 = g.addNode("A");
        // AdjacencyMapGraph currently returns a new instance due to HashMap.put returning new instance
        Assert.assertEquals(g, g2); 
        
        // Edge case: addEdge with existing edge
        Graph<String> g3 = g.addEdge("A", "B");
        Graph<String> g4 = g3.addEdge("A", "B");
        Assert.assertEquals(g3, g4);
    }
}
