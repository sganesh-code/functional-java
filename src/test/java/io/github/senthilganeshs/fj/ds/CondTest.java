package io.github.senthilganeshs.fj.ds;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class CondTest {

    @Test
    public void testCondAccumulation() {
        List<String> results = Cond.<String>empty()
            .when(true, "A")
            .when(false, "B")
            .when(true, "C")
            .evaluate();
        
        System.out.println("DEBUG COND RESULTS: " + results);
        assertEquals(results.length(), 2);
        assertEquals(results.atIndex(0).orElse(""), "A");
        assertEquals(results.atIndex(1).orElse(""), "C");
    }

    @Test
    public void testEmptyCond() {
        List<Integer> results = Cond.<Integer>empty()
            .when(false, 1)
            .evaluate();
        
        assertTrue(results.isEmpty());
    }
}
