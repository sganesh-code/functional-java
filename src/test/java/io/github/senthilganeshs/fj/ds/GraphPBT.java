package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.test.Gen;
import io.github.senthilganeshs.fj.test.Property;
import io.github.senthilganeshs.fj.typeclass.Hashable;
import org.testng.Assert;
import org.testng.annotations.Test;

public class GraphPBT {

    @Test
    public void testGraphTopologicalInvariants() {
        Gen<Graph<Integer>> graphGen = Gen.choose(1, 20).flatMap(n -> {
            Graph<Integer> g = Graph.nil();
            for (int i = 0; i < n; i++) g = g.addVertex(i);
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    if (Math.random() > 0.7) g = g.addEdge(i, j);
                }
            }
            return Gen.pure(g);
        });

        Property.forAll(graphGen, g -> {
            Maybe<List<Integer>> sorted = g.topologicalSort();
            if (sorted.isNothing()) return true;
            
            List<Integer> list = sorted.orElse(List.nil());
            // Verify topological order: for every edge (u, v), u comes before v
            return g.vertices().all(u -> 
                g.neighbors(u).all(v -> 
                    list.indexOf(u).orElse(-1) < list.indexOf(v).orElse(-1)
                )
            );
        }).assertTrue(50);
    }

    @Test
    public void testHashable() {
        Hashable<Integer> intHash = Object::toString;
        Assert.assertEquals(intHash.hash(1), "1");
    }
}
