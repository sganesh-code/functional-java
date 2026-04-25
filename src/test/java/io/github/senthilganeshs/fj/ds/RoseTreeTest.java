package io.github.senthilganeshs.fj.ds;

import org.testng.Assert;
import org.testng.annotations.Test;

public class RoseTreeTest {

    @Test
    public void testRoseTreeBasic() {
        RoseTree<String> tree = RoseTree.of("root", 
            List.of(
                RoseTree.of("c1"),
                RoseTree.of("c2")
            )
        );
        
        Assert.assertEquals(tree.value(), "root");
        Assert.assertEquals(tree.children().length(), 2);
    }

    @Test
    public void testRoseTreeLeaf() {
        RoseTree<String> leaf = RoseTree.of("leaf");
        Assert.assertEquals(leaf.value(), "leaf");
        Assert.assertEquals(leaf.children().length(), 0);
    }

    @Test
    public void testRoseTreeFoldl() {
        // Pre-order: root, c1, g1, c2
        RoseTree<String> tree = RoseTree.of("root", 
            List.of(
                RoseTree.of("c1", List.of(RoseTree.of("g1"))),
                RoseTree.of("c2")
            )
        );
        
        String result = tree.foldl("", (acc, v) -> acc + (acc.isEmpty() ? "" : ",") + v);
        Assert.assertEquals(result, "root,c1,g1,c2");
    }

    @Test
    public void testRoseTreeMap() {
        RoseTree<Integer> tree = RoseTree.of(1, 
            List.of(
                RoseTree.of(2, List.of(RoseTree.of(3))),
                RoseTree.of(4)
            )
        );
        
        RoseTree<Integer> doubled = (RoseTree<Integer>) tree.map(i -> i * 2);
        Assert.assertEquals(doubled.value(), Integer.valueOf(2));
        
        String result = doubled.foldl("", (acc, v) -> acc + (acc.isEmpty() ? "" : ",") + v);
        Assert.assertEquals(result, "2,4,6,8");
    }

    @Test
    public void testRoseTreeBuild() {
        // build(input) adds a child to the root in RoseTree implementation
        RoseTree<String> tree = RoseTree.of("root");
        tree = (RoseTree<String>) tree.build("child");
        
        Assert.assertEquals(tree.children().length(), 1);
        Assert.assertEquals(tree.children().find(i -> true).orElse(RoseTree.of("")).value(), "child");
    }

    @Test
    public void testRoseTreeStatic() {
        RoseTree<Integer> t1 = RoseTree.of(1);
        RoseTree<Integer> t2 = RoseTree.of(1, List.nil());
        Assert.assertEquals(t1, t2);
    }
}
