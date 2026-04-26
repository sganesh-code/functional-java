package io.github.senthilganeshs.fj.parser;

import io.github.senthilganeshs.fj.ds.*;
import io.github.senthilganeshs.fj.parser.JsonValue.*;
import java.util.function.Predicate;

/**
 * A complete JSON parser implemented using Parser Combinators.
 */
public class JsonParser {

    private static final Parser<List<Character>> whitespace = Parser.whitespace().many();

    private static <A> Parser<A> lexeme(Parser<A> p) {
        return p.ignore(whitespace);
    }

    private static final Parser<Character> lbrace = lexeme(Parser.character('{'));
    private static final Parser<Character> rbrace = lexeme(Parser.character('}'));
    private static final Parser<Character> lbracket = lexeme(Parser.character('['));
    private static final Parser<Character> rbracket = lexeme(Parser.character(']'));
    private static final Parser<Character> comma = lexeme(Parser.character(','));
    private static final Parser<Character> colon = lexeme(Parser.character(':'));

    private static final Parser<JsonValue> jNull = lexeme(Parser.string("null"))
        .map(v -> new JsonNull());

    private static final Parser<JsonValue> jBool = lexeme(
        Parser.string("true").<JsonValue>map(v -> new JsonBoolean(true))
        .or(Parser.string("false").map(v -> new JsonBoolean(false)))
    );

    private static final Parser<JsonValue> jNumber = lexeme(state -> {
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

        if (!hasDigits) return Either.left(new ParseError(state.position(), "Expected number"));
        
        try {
            double d = Double.parseDouble(sb.toString());
            return Either.right(Tuple.of(new JsonNumber(d), state.advance(pos - state.position())));
        } catch (NumberFormatException e) {
            return Either.left(new ParseError(state.position(), "Invalid number format"));
        }
    });

    private static final Parser<String> jStringRaw = state -> {
        if (state.isEOF() || state.source().charAt(state.position()) != '"') {
            return Either.left(new ParseError(state.position(), "Expected '\"'"));
        }
        
        StringBuilder sb = new StringBuilder();
        int pos = state.position() + 1; // skip initial quote
        String src = state.source();
        
        while (pos < src.length() && src.charAt(pos) != '"') {
            char c = src.charAt(pos);
            if (c == '\\') {
                pos++;
                if (pos >= src.length()) return Either.left(new ParseError(pos, "Unexpected EOF in string escape"));
                char escape = src.charAt(pos);
                switch (escape) {
                    case '"'  -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/'  -> sb.append('/');
                    case 'b'  -> sb.append('\b');
                    case 'f'  -> sb.append('\f');
                    case 'n'  -> sb.append('\n');
                    case 'r'  -> sb.append('\r');
                    case 't'  -> sb.append('\t');
                    case 'u'  -> {
                        if (pos + 4 >= src.length()) return Either.left(new ParseError(pos, "Invalid unicode escape"));
                        String hex = src.substring(pos + 1, pos + 5);
                        sb.append((char) Integer.parseInt(hex, 16));
                        pos += 4;
                    }
                    default -> sb.append(escape);
                }
            } else {
                sb.append(c);
            }
            pos++;
        }
        
        if (pos >= src.length()) return Either.left(new ParseError(pos, "Unterminated string"));
        return Either.right(Tuple.of(sb.toString(), state.advance(pos + 1 - state.position())));
    };

    private static final Parser<JsonValue> jString = lexeme(jStringRaw).map(JsonString::new);

    private static final Parser<JsonValue> jValue = Parser.lazy(() -> 
        jNull.or(jBool).or(jNumber).or(jString).or(jArray()).or(jObject())
    );

    private static Parser<JsonValue> jArray() {
        return jValue.sepBy(comma).between(lbracket, rbracket)
            .map(JsonArray::new);
    }

    private static Parser<JsonValue> jObject() {
        Parser<Tuple<String, JsonValue>> pair = lexeme(jStringRaw).ignore(colon).and(jValue);
        
        return pair.sepBy(comma).between(lbrace, rbrace)
            .map(pairs -> {
                HashMap<String, JsonValue> map = pairs.foldl(HashMap.nil(), 
                    (m, t) -> m.put(t.getA().orElse(""), t.getB().orElse(new JsonNull())));
                return new JsonObject(map);
            });
    }

    public static Parser<JsonValue> parser() {
        return whitespace.then(jValue).ignore(whitespace).ignore(Parser.eof());
    }
}
