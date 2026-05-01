package io.github.senthilganeshs.fj.parser;

import io.github.senthilganeshs.fj.ds.Either;
import io.github.senthilganeshs.fj.ds.HashMap;
import io.github.senthilganeshs.fj.ds.List;
import io.github.senthilganeshs.fj.ds.Maybe;
import io.github.senthilganeshs.fj.ds.Tuple;
import io.github.senthilganeshs.fj.parser.JsonValue.*;
import java.util.function.Function;

/**
 * A purely functional JSON parser implementation.
 */
public final class JsonParser {

    private JsonParser() {}

    private static final Parser<JsonValue> jsonNull = Parser.string("null").map(__ -> (JsonValue) new JsonNull()).lexeme();

    private static final Parser<JsonValue> jsonBoolean = Parser.string("true").map(__ -> (JsonValue) new JsonBoolean(true))
        .or(Parser.string("false").map(__ -> (JsonValue) new JsonBoolean(false)))
        .lexeme();

    private static final Parser<JsonValue> jsonNumber = state -> {
        StringBuilder sb = new StringBuilder();
        int pos = state.position();
        String src = state.source();
        
        if (pos < src.length() && src.charAt(pos) == '-') {
            sb.append('-');
            pos++;
        }
        
        boolean hasDigits = false;
        while (pos < src.length() && Character.isDigit(src.charAt(pos))) {
            sb.append(src.charAt(pos));
            pos++;
            hasDigits = true;
        }
        
        if (pos < src.length() && src.charAt(pos) == '.') {
            sb.append('.');
            pos++;
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) {
                sb.append(src.charAt(pos));
                pos++;
            }
        }

        if (pos < src.length() && (src.charAt(pos) == 'e' || src.charAt(pos) == 'E')) {
            sb.append(src.charAt(pos));
            pos++;
            if (pos < src.length() && (src.charAt(pos) == '+' || src.charAt(pos) == '-')) {
                sb.append(src.charAt(pos));
                pos++;
            }
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) {
                sb.append(src.charAt(pos));
                pos++;
            }
        }

        if (!hasDigits) return Either.left(new ParseError(state.position(), state.line(), state.column(), "Expected number"));
        
        double val = Double.parseDouble(sb.toString());
        return Either.right(Tuple.of((JsonValue) new JsonNumber(val), state.advance(sb.length())));
    };

    private static final Parser<String> jsonStringRaw = Parser.character('"')
        .then(state -> {
            StringBuilder sb = new StringBuilder();
            int pos = state.position();
            String src = state.source();
            while (pos < src.length() && src.charAt(pos) != '"') {
                char c = src.charAt(pos);
                if (c == '\\') {
                    pos++;
                    if (pos >= src.length()) return Either.left(new ParseError(pos, state.line(), state.column(), "Unexpected EOF in string escape"));
                    char esc = src.charAt(pos);
                    switch (esc) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        default -> sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
                pos++;
            }
            if (pos >= src.length()) return Either.left(new ParseError(pos, state.line(), state.column(), "Unterminated string"));
            return Either.right(Tuple.of(sb.toString(), state.advance(pos - state.position() + 1)));
        });

    private static final Parser<JsonValue> jsonString = jsonStringRaw.map(s -> (JsonValue) new JsonString(s)).lexeme();

    private static final Parser<JsonValue> jsonArray = Parser.lazy(() -> 
        jsonValue()
            .sepBy(Parser.character(',').lexeme())
            .between(Parser.character('[').lexeme(), Parser.character(']').lexeme())
            .map(JsonArray::new)
    );

    private static final Parser<JsonValue> jsonObject = Parser.lazy(() -> 
        jsonStringRaw.lexeme()
            .ignore(Parser.character(':').lexeme())
            .and(jsonValue())
            .sepBy(Parser.character(',').lexeme())
            .between(Parser.character('{').lexeme(), Parser.character('}').lexeme())
            .map(pairs -> {
                final HashMap<String, JsonValue>[] fields = new HashMap[]{HashMap.nil()};
                pairs.forEach(pair -> {
                    fields[0] = fields[0].put(pair.getA().orElse(""), pair.getB().orElse(null));
                });
                return new JsonObject(fields[0]);
            })
    );

    private static Parser<JsonValue> jsonValue() {
        return jsonNull.or(jsonBoolean).or(jsonNumber).or(jsonString).or(jsonArray).or(jsonObject);
    }

    public static Parser<JsonValue> parser() {
        return Parser.spaces().then(jsonValue());
    }
}
