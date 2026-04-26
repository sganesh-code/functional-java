package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.optic.Lens;
import io.github.senthilganeshs.fj.optic.RecordOptics;
import org.testng.Assert;
import org.testng.annotations.Test;

public class RecordOpticsTest {

    record Player(String name, int score) {}

    @Test
    public void testAutomatedLens() {
        // Automatically generate Lenses using method references
        Lens<Player, String> nameL = RecordOptics.of(Player.class, Player::name);
        Lens<Player, Integer> scoreL = RecordOptics.of(Player.class, Player::score);

        Player p1 = new Player("Alice", 100);

        // Test Get
        Assert.assertEquals(nameL.get(p1), "Alice");
        Assert.assertEquals(scoreL.get(p1), Integer.valueOf(100));

        // Test Set (Persistence)
        Player p2 = nameL.set("Bob", p1);
        Assert.assertEquals(p2.name(), "Bob");
        Assert.assertEquals(p2.score(), 100);
        Assert.assertEquals(p1.name(), "Alice"); // Original unchanged

        // Test Modify
        Player p3 = scoreL.modify(p1, s -> s + 50);
        Assert.assertEquals(p3.score(), 150);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNonRecordFailure() {
        RecordOptics.of(String.class, s -> s.length());
    }
}
