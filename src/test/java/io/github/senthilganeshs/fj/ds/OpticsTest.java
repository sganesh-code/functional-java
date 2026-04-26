package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.typeclass.*;

import io.github.senthilganeshs.fj.optic.AffineTraversal;
import io.github.senthilganeshs.fj.optic.Lens;
import io.github.senthilganeshs.fj.optic.Prism;
import io.github.senthilganeshs.fj.optic.Traversal;
import org.testng.Assert;
import org.testng.annotations.Test;

public class OpticsTest {

    record Address(String city) {}
    record User(String name, Address addr) {}

    @Test
    public void testLens() {
        Lens<Address, String> cityL = Lens.of(Address::city, (c, a) -> new Address(c));
        Lens<User, Address> addrL = Lens.of(User::addr, (a, u) -> new User(u.name(), a));
        
        Lens<User, String> userCityL = addrL.compose(cityL);
        
        User u = new User("Alice", new Address("London"));
        Assert.assertEquals(userCityL.get(u), "London");
        
        User updated = userCityL.set("Paris", u);
        Assert.assertEquals(updated.addr().city(), "Paris");
        Assert.assertEquals(u.addr().city(), "London"); // Persistence
    }

    @Test
    public void testPrism() {
        Prism<Either<String, Integer>, Integer> rightP = Prism.of(
            e -> e.isRight() ? Maybe.some(e.orElse(null)) : Maybe.nothing(),
            Either::right
        );
        
        Either<String, Integer> r = Either.right(10);
        Either<String, Integer> l = Either.left("err");
        
        Assert.assertEquals(rightP.getMaybe(r).orElse(-1), Integer.valueOf(10));
        Assert.assertTrue(rightP.getMaybe(l).isNothing());
        
        Either<String, Integer> res = rightP.set(20, r);
        Assert.assertEquals(res.orElse(-1), Integer.valueOf(20));
        Assert.assertEquals(rightP.set(20, l), l); // Prism doesn't modify if focus is missing
    }

    @Test
    public void testIndexedOptics() {
        Collection<String> list = List.of("A", "B", "C");
        
        // Focus on the second element (index 1)
        AffineTraversal<Collection<String>, String> secondP = Collection.at(1);
        
        Assert.assertEquals(secondP.getMaybe(list).orElse(""), "B");
        
        Collection<String> updated = secondP.set("X", list);
        Assert.assertEquals(updated.toString(), "[A,X,C]");
    }
}
