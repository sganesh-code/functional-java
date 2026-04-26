package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.optic.AffineTraversal;
import io.github.senthilganeshs.fj.optic.Lens;
import io.github.senthilganeshs.fj.optic.Prism;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AffineTraversalTest {

    record Address(String city) {}
    record User(String name, Maybe<Address> address) {}

    @Test
    public void testAffineTraversalWithDefault() {
        Lens<Address, String> cityL = Lens.of(Address::city, (c, a) -> new Address(c));
        Prism<Maybe<Address>, Address> addressP = Maybe.someP();
        Lens<User, Maybe<Address>> userAddrL = Lens.of(User::address, (a, u) -> new User(u.name(), a));

        // User -> Maybe Address -> City
        AffineTraversal<User, String> cityT = userAddrL.compose(addressP).compose(cityL);

        // Convert to mandatory Lens by providing a default
        Lens<User, String> cityWithDefaultL = cityT.withDefault("UNKNOWN");

        User u1 = new User("Alice", Maybe.some(new Address("London")));
        User u2 = new User("Bob", Maybe.nothing());

        // Test Get
        Assert.assertEquals(cityWithDefaultL.get(u1), "London");
        Assert.assertEquals(cityWithDefaultL.get(u2), "UNKNOWN");

        // Test Set
        User u3 = cityWithDefaultL.set("Paris", u1);
        Assert.assertEquals(u3.address().orElse(null).city(), "Paris");

        User u4 = cityWithDefaultL.set("Paris", u2);
        Assert.assertEquals(u4, u2); // No address to set, returns original
    }
}
