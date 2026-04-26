package io.github.senthilganeshs.fj.parser;

import io.github.senthilganeshs.fj.ds.HashMap;
import io.github.senthilganeshs.fj.ds.List;

/**
 * A purely functional JSON AST.
 */
public sealed interface JsonValue {
    record JsonObject(HashMap<String, JsonValue> fields) implements JsonValue {}
    record JsonArray(List<JsonValue> elements) implements JsonValue {}
    record JsonString(String value) implements JsonValue {}
    record JsonNumber(double value) implements JsonValue {}
    record JsonBoolean(boolean value) implements JsonValue {}
    record JsonNull() implements JsonValue {}
}
