package io.github.senthilganeshs.fj.parser;

import io.github.senthilganeshs.fj.ds.List;
import io.github.senthilganeshs.fj.ds.Maybe;
import io.github.senthilganeshs.fj.optic.Iso;
import io.github.senthilganeshs.fj.optic.RecordOptics;
import org.testng.Assert;
import org.testng.annotations.Test;

public class JsonIsoTest {

    public record Profile(String bio, int age) {}
    public record User(String name, Profile profile, List<String> roles, Maybe<String> nickname) {}

    @Test
    public void testJsonIsoRoundTrip() {
        User user = new User(
            "Alice",
            new Profile("Software Engineer", 30),
            List.of("ADMIN", "USER"),
            Maybe.some("Ali")
        );

        Iso<User, JsonValue> userIso = RecordOptics.jsonIso(User.class);

        // 1. Record -> JSON
        JsonValue json = userIso.get(user);
        Assert.assertTrue(json instanceof JsonValue.JsonObject);

        // 2. JSON -> Record
        User decoded = userIso.reverseGet(json);

        Assert.assertEquals(decoded.name(), user.name());
        Assert.assertEquals(decoded.profile().bio(), user.profile().bio());
        Assert.assertEquals(decoded.profile().age(), user.profile().age());
        Assert.assertEquals(decoded.roles().length(), 2);
        Assert.assertEquals(decoded.roles().drop(0).headMaybe().orElse(""), "ADMIN");
        Assert.assertEquals(decoded.nickname(), user.nickname());
    }

    @Test
    public void testJsonIsoWithNulls() {
        User user = new User(
            "Bob",
            new Profile(null, 25),
            List.nil(),
            Maybe.nothing()
        );

        User decoded = JsonValue.fromRecord(user).toRecord(User.class);

        Assert.assertEquals(decoded.name(), "Bob");
        Assert.assertNull(decoded.profile().bio());
        Assert.assertEquals(decoded.roles().length(), 0);
        Assert.assertTrue(decoded.nickname().isNothing());
    }
}
