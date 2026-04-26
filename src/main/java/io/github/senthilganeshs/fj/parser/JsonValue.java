package io.github.senthilganeshs.fj.parser;

import io.github.senthilganeshs.fj.ds.HashMap;
import io.github.senthilganeshs.fj.ds.List;
import io.github.senthilganeshs.fj.ds.Maybe;
import io.github.senthilganeshs.fj.optic.Prism;

/**
 * A purely functional JSON AST with built-in optics.
 */
public sealed interface JsonValue {
    record JsonObject(HashMap<String, JsonValue> fields) implements JsonValue {}
    record JsonArray(List<JsonValue> elements) implements JsonValue {}
    record JsonString(String value) implements JsonValue {}
    record JsonNumber(double value) implements JsonValue {}
    record JsonBoolean(boolean value) implements JsonValue {}
    record JsonNull() implements JsonValue {}

    // --- Prisms ---

    static Prism<JsonValue, JsonObject> objectP() {
        return Prism.of(
            v -> v instanceof JsonObject o ? Maybe.some(o) : Maybe.nothing(),
            o -> o
        );
    }

    static Prism<JsonValue, JsonArray> arrayP() {
        return Prism.of(
            v -> v instanceof JsonArray a ? Maybe.some(a) : Maybe.nothing(),
            a -> a
        );
    }

    static Prism<JsonValue, String> stringP() {
        return Prism.of(
            v -> v instanceof JsonString s ? Maybe.some(s.value()) : Maybe.nothing(),
            JsonString::new
        );
    }

    static Prism<JsonValue, Double> numberP() {
        return Prism.of(
            v -> v instanceof JsonNumber n ? Maybe.some(n.value()) : Maybe.nothing(),
            JsonNumber::new
        );
    }

    static Prism<JsonValue, Boolean> booleanP() {
        return Prism.of(
            v -> v instanceof JsonBoolean b ? Maybe.some(b.value()) : Maybe.nothing(),
            JsonBoolean::new
        );
    }
}
