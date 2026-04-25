package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.typeclass.Monoid;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ValidationTest {

    @Test
    public void testErrorAccumulation() {
        Validation<String, Integer> v1 = Validation.invalid("Error 1;");
        Validation<String, Integer> v2 = Validation.invalid("Error 2;");
        
        // Use STRING_CONCAT monoid to combine error messages
        Validation<String, Integer> combined = v1.liftA2(Integer::sum, v2, Monoid.STRING_CONCAT);
        
        Assert.assertFalse(combined.isValid());
        Assert.assertEquals(combined.toString(), "Invalid(Error 1;Error 2;)");
    }

    @Test
    public void testSuccessCombination() {
        Validation<String, Integer> v1 = Validation.valid(10);
        Validation<String, Integer> v2 = Validation.valid(20);
        
        Validation<String, Integer> combined = v1.liftA2(Integer::sum, v2, Monoid.STRING_CONCAT);
        
        Assert.assertTrue(combined.isValid());
        Assert.assertEquals(combined.orElse(0), Integer.valueOf(30));
    }
}
