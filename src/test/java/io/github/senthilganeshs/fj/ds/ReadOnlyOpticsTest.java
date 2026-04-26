package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.optic.Fold;
import io.github.senthilganeshs.fj.optic.Getter;
import io.github.senthilganeshs.fj.optic.Lens;
import io.github.senthilganeshs.fj.optic.Traversal;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ReadOnlyOpticsTest {

    record User(String name, int age) {}

    @Test
    public void testGetter() {
        Getter<User, String> nameG = User::name;
        User u = new User("Alice", 30);
        Assert.assertEquals(nameG.get(u), "Alice");
    }

    @Test
    public void testFold() {
        Fold<Collection<User>, String> eachNameF = s -> s.map(User::name);
        Collection<User> users = List.of(new User("A", 10), new User("B", 20));
        
        Collection<String> names = eachNameF.getAll(users);
        Assert.assertEquals(names.toString(), "[A,B]");
    }

    @Test
    public void testComposition() {
        Lens<User, String> nameL = Lens.of(User::name, (n, u) -> new User(n, u.age()));
        Traversal<Collection<User>, User> eachUser = Traversal.fromCollection();
        
        // Lens -> Getter
        Getter<User, Integer> nameLengthG = nameL.asGetter().compose(String::length);
        Assert.assertEquals(nameLengthG.get(new User("Alice", 30)), Integer.valueOf(5));

        // Traversal -> Fold
        Fold<Collection<User>, Integer> agesF = eachUser.asFold().compose(Getter.of(u -> u.age));
        Collection<Integer> ages = agesF.getAll(List.of(new User("A", 10), new User("B", 20)));
        Assert.assertEquals(ages.toString(), "[10,20]");
    }
}
