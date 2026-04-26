package io.github.senthilganeshs.fj.parser;

import io.github.senthilganeshs.fj.ds.*;
import io.github.senthilganeshs.fj.parser.JsonValue.*;
import io.github.senthilganeshs.fj.optic.*;
import org.testng.Assert;
import org.testng.annotations.Test;

public class JsonParserTest {

    @Test
    public void testPrimitives() {
        JsonValue v1 = JsonParser.parser().parse("null").orElse(null);
        Assert.assertTrue(v1 instanceof JsonNull);

        JsonValue v2 = JsonParser.parser().parse("true").orElse(null);
        Assert.assertEquals(((JsonBoolean) v2).value(), true);

        JsonValue v3 = JsonParser.parser().parse("false").orElse(null);
        Assert.assertEquals(((JsonBoolean) v3).value(), false);

        JsonValue v4 = JsonParser.parser().parse("123.45").orElse(null);
        Assert.assertEquals(((JsonNumber) v4).value(), 123.45);

        JsonValue v5 = JsonParser.parser().parse("-1e2").orElse(null);
        Assert.assertEquals(((JsonNumber) v5).value(), -100.0);

        JsonValue v6 = JsonParser.parser().parse("\"hello world\"").orElse(null);
        Assert.assertEquals(((JsonString) v6).value(), "hello world");
    }

    @Test
    public void testStringEscapes() {
        JsonValue v = JsonParser.parser().parse("\"line1\\nline2\\t\\\"quoted\\\"\"").orElse(null);
        Assert.assertEquals(((JsonString) v).value(), "line1\nline2\t\"quoted\"");
    }

    @Test
    public void testCollections() {
        String json = """
            {
                "id": 1,
                "name": "Alice",
                "scores": [85, 92, 78],
                "active": true,
                "meta": null
            }
            """;
        
        JsonValue v = JsonParser.parser().parse(json).orElse(null);
        Assert.assertTrue(v instanceof JsonObject);
        JsonObject obj = (JsonObject) v;
        
        Assert.assertEquals(((JsonNumber) obj.fields().get("id").orElse(null)).value(), 1.0);
        Assert.assertEquals(((JsonString) obj.fields().get("name").orElse(null)).value(), "Alice");
        
        JsonArray scores = (JsonArray) obj.fields().get("scores").orElse(null);
        Assert.assertEquals(scores.elements().length(), 3);
        Assert.assertEquals(((JsonNumber) scores.elements().drop(0).headMaybe().orElse(null)).value(), 85.0);
        
        Assert.assertEquals(((JsonBoolean) obj.fields().get("active").orElse(null)).value(), true);
        Assert.assertTrue(obj.fields().get("meta").orElse(null) instanceof JsonNull);
    }

    @Test
    public void testNested() {
        String json = "[{\"a\": [1, 2]}, {\"b\": 3}]";
        JsonValue v = JsonParser.parser().parse(json).orElse(null);
        Assert.assertTrue(v instanceof JsonArray);
        JsonArray arr = (JsonArray) v;
        Assert.assertEquals(arr.elements().length(), 2);
    }

    @Test
    public void testDeepUpdateWithOptics() {
        String json = "{\"user\": {\"profile\": {\"name\": \"Alice\"}}}";
        JsonValue v = JsonParser.parser().parse(json).orElse(null);

        // Define an optic that focuses on user.profile.name using the new navigation helpers
        var nameLens = JsonValue.path("user", "profile").compose(JsonValue.stringAt("name"));

        JsonValue updated = nameLens.set("Bob", v);
        
        Assert.assertEquals(nameLens.getMaybe(updated).orElse(""), "Bob");
        Assert.assertEquals(nameLens.getMaybe(v).orElse(""), "Alice");
    }

    @Test
    public void testPolymorphicPath() {
        String json = """
            {
                "users": [
                    {"name": "Alice"},
                    {"name": "Bob"}
                ]
            }
            """;
        JsonValue v = JsonParser.parser().parse(json).orElse(null);

        // Navigate via mixed steps: key "users", index 1, key "name"
        var bobNameLens = JsonValue.path("users", 1, "name").compose(JsonValue.stringP());
        
        Assert.assertEquals(bobNameLens.getMaybe(v).orElse(""), "Bob");
        
        JsonValue updated = bobNameLens.set("Robert", v);
        Assert.assertEquals(bobNameLens.getMaybe(updated).orElse(""), "Robert");
    }

    @Test
    public void testPathValidation() {
        String json = "{\"users\": [{\"name\": \"Alice\"}]}";
        JsonValue v = JsonParser.parser().parse(json).orElse(null);

        // Valid path
        Assert.assertTrue(v.validatePath("users", 0, "name").isValid());

        // Invalid: missing key
        Validation<String, JsonValue> v1 = v.validatePath("users", 0, "age");
        Assert.assertFalse(v1.isValid());
        Assert.assertTrue(v1.getErrors().orElse("").contains("Key 'age' not found"));

        // Invalid: wrong type (expecting array but found object)
        Validation<String, JsonValue> v2 = v.validatePath("users", "name");
        Assert.assertFalse(v2.isValid());
        Assert.assertTrue(v2.getErrors().orElse("").contains("Expected JsonObject"));

        // Invalid: index out of bounds
        Validation<String, JsonValue> v3 = v.validatePath("users", 5);
        Assert.assertFalse(v3.isValid());
        Assert.assertTrue(v3.getErrors().orElse("").contains("Index 5 out of bounds"));
    }

    @Test
    public void testInvalidJson() {
        Either<ParseError, JsonValue> res = JsonParser.parser().parse("{ \"unclosed\": [1, 2 }");
        Assert.assertTrue(res.isLeft());
        Assert.assertTrue(res.fromLeft(null).message().contains("Expected '}'"));
    }
}
