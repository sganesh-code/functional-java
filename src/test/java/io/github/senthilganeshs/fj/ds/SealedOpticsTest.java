package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.optic.Prism;
import io.github.senthilganeshs.fj.optic.SealedOptics;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SealedOpticsTest {

    sealed interface Shape permits Circle, Square {}
    record Circle(double radius) implements Shape {}
    record Square(double side) implements Shape {}

    @Test
    public void testAutomatedPrism() {
        // Automatically generate Prisms for sealed interface cases
        Prism<Shape, Circle> circleP = SealedOptics.prism(Shape.class, Circle.class);
        Prism<Shape, Square> squareP = SealedOptics.prism(Shape.class, Square.class);

        Shape s1 = new Circle(5.0);
        Shape s2 = new Square(10.0);

        // Test Get (Preview)
        Assert.assertTrue(circleP.getMaybe(s1).isSome());
        Assert.assertEquals(circleP.getMaybe(s1).orElse(null).radius(), 5.0);
        Assert.assertTrue(circleP.getMaybe(s2).isNothing());

        // Test ReverseGet (Review)
        Circle c = new Circle(3.0);
        Shape s3 = circleP.reverseGet(c);
        Assert.assertEquals(s3, c);
    }
}
