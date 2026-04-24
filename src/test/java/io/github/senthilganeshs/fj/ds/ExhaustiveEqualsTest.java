package io.github.senthilganeshs.fj.ds;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ExhaustiveEqualsTest {

    @Test
    public void testEqualsBasic() {
        Assert.assertEquals(Maybe.some(1), Maybe.some(1));
    }
}
