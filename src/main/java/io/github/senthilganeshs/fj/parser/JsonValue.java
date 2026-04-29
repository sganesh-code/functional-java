package io.github.senthilganeshs.fj.parser;

import io.github.senthilganeshs.fj.ds.Collection;
import io.github.senthilganeshs.fj.ds.HashMap;
import io.github.senthilganeshs.fj.ds.List;
import io.github.senthilganeshs.fj.ds.Maybe;
import io.github.senthilganeshs.fj.ds.Validation;
import io.github.senthilganeshs.fj.optic.AffineTraversal;
import io.github.senthilganeshs.fj.optic.Prism;
import io.github.senthilganeshs.fj.optic.RecordOptics;

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

    // --- Navigation Helpers ---

    static AffineTraversal<JsonValue, JsonValue> at(String key) {
        return objectP()
            .compose(RecordOptics.of(JsonObject.class, JsonObject::fields))
            .compose(HashMap.<String, JsonValue>nil().at(key));
    }

    static AffineTraversal<JsonValue, JsonValue> index(int i) {
        return arrayP()
            .compose(RecordOptics.of(JsonArray.class, JsonArray::elements))
            .compose((AffineTraversal<List<JsonValue>, JsonValue>) (AffineTraversal<?, ?>) Collection.<JsonValue>at(i));
    }

    static AffineTraversal<JsonValue, String> stringAt(String key) { return at(key).compose(stringP()); }
    static AffineTraversal<JsonValue, Double> numberAt(String key) { return at(key).compose(numberP()); }
    static AffineTraversal<JsonValue, Boolean> booleanAt(String key) { return at(key).compose(booleanP()); }
    static AffineTraversal<JsonValue, JsonObject> objectAt(String key) { return at(key).compose(objectP()); }
    static AffineTraversal<JsonValue, JsonArray> arrayAt(String key) { return at(key).compose(arrayP()); }

    static AffineTraversal<JsonValue, JsonValue> path(Object... steps) {
        AffineTraversal<JsonValue, JsonValue> res = AffineTraversal.identity();
        for (Object step : steps) {
            if (step instanceof String s) res = res.compose(at(s));
            else if (step instanceof Integer i) res = res.compose(index(i));
        }
        return res;
    }

    default Validation<String, JsonValue> validatePath(Object... steps) {
        JsonValue current = this;
        for (int i = 0; i < steps.length; i++) {
            Object step = steps[i];
            if (step instanceof String s) {
                if (!(current instanceof JsonObject obj)) {
                    return Validation.invalid("Expected JsonObject at step " + i + " ('" + s + "'), but found " + current.getClass().getSimpleName());
                }
                Maybe<JsonValue> next = obj.fields().get(s);
                if (next.isNothing()) return Validation.invalid("Key '" + s + "' not found at step " + i);
                current = next.orElse(null);
            } else if (step instanceof Integer idx) {
                if (!(current instanceof JsonArray arr)) {
                    return Validation.invalid("Expected JsonArray at step " + i + " (" + idx + "), but found " + current.getClass().getSimpleName());
                }
                Maybe<JsonValue> next = arr.elements().drop(idx).headMaybe();
                if (next.isNothing()) return Validation.invalid("Index " + idx + " out of bounds at step " + i);
                current = next.orElse(null);
            }
        }
        return Validation.valid(current);
    }

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

    // --- Record Conversion ---

    default <R> R toRecord(Class<R> recordClass) {
        return RecordOptics.jsonIso(recordClass).reverseGet(this);
    }

    static <R> JsonValue fromRecord(R record) {
        return (JsonValue) RecordOptics.jsonIso((Class<R>) record.getClass()).get(record);
    }

    // --- Serialization ---

    default String serialize() {
        if (this instanceof JsonObject o) {
            StringBuilder sb = new StringBuilder("{");
            o.fields().forEach(entry -> {
                if (sb.length() > 1) sb.append(",");
                sb.append("\"").append(escape(entry.key())).append("\":")
                  .append(entry.value().serialize());
            });
            return sb.append("}").toString();
        } else if (this instanceof JsonArray a) {
            StringBuilder sb = new StringBuilder("[");
            a.elements().forEach(val -> {
                if (sb.length() > 1) sb.append(",");
                sb.append(val.serialize());
            });
            return sb.append("]").toString();
        } else if (this instanceof JsonString s) {
            return "\"" + escape(s.value()) + "\"";
        } else if (this instanceof JsonNumber n) {
            double v = n.value();
            if (v == (long) v) return String.valueOf((long) v);
            return String.valueOf(v);
        } else if (this instanceof JsonBoolean b) {
            return String.valueOf(b.value());
        } else {
            return "null";
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
