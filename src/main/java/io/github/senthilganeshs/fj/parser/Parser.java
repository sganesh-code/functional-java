package io.github.senthilganeshs.fj.parser;

import io.github.senthilganeshs.fj.ds.Either;
import io.github.senthilganeshs.fj.ds.List;
import io.github.senthilganeshs.fj.ds.Maybe;
import io.github.senthilganeshs.fj.ds.Tuple;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Foundation for purely functional parser combinators.
 */
public interface Parser<A> {

    Either<ParseError, Tuple<A, State>> parse(State state);

    default Either<ParseError, A> parse(String input) {
        return parse(new State(input, 0, 1, 1)).map(t -> t.getA().orElse(null));
    }

    // --- Combinators ---

    default <B> Parser<B> map(Function<A, B> fn) {
        return state -> parse(state).map(t -> t.mapA(fn));
    }

    default <B> Parser<B> flatMap(Function<A, Parser<B>> fn) {
        return state -> parse(state).flatMapEither(t -> fn.apply(t.getA().orElse(null)).parse(t.getB().orElse(null)));
    }

    default Parser<A> or(Parser<A> other) {
        return state -> {
            Either<ParseError, Tuple<A, State>> res = parse(state);
            return res.isRight() ? res : other.parse(state);
        };
    }

    default <B> Parser<Tuple<A, B>> and(Parser<B> other) {
        return this.flatMap(a -> other.map(b -> Tuple.of(a, b)));
    }

    default <B> Parser<A> ignore(Parser<B> other) {
        return this.flatMap(a -> other.map(__ -> a));
    }

    default <B> Parser<B> then(Parser<B> other) {
        return this.flatMap(__ -> other);
    }

    default Parser<List<A>> many() {
        return state -> {
            List<A> results = List.nil();
            State currentState = state;
            while (true) {
                Either<ParseError, Tuple<A, State>> res = parse(currentState);
                if (res.isLeft()) break;
                Tuple<A, State> t = res.fromRight(null);
                
                State nextState = t.getB().orElse(null);
                if (nextState.position() <= currentState.position()) break;

                results = (List<A>) results.build(t.getA().orElse(null));
                currentState = nextState;
            }
            return Either.<ParseError, Tuple<List<A>, State>>right(Tuple.of(results, currentState));
        };
    }

    default Parser<List<A>> many1() {
        return this.flatMap(first -> 
            this.many().map(rest -> (List<A>) rest.reverse().build(first).reverse())
        );
    }

    default Parser<Maybe<A>> optional() {
        return state -> {
            Either<ParseError, Tuple<A, State>> res = parse(state);
            if (res.isRight()) {
                Tuple<A, State> t = res.fromRight(null);
                return Either.right(Tuple.of(Maybe.some(t.getA().orElse(null)), t.getB().orElse(null)));
            }
            return Either.right(Tuple.of(Maybe.nothing(), state));
        };
    }

    default <B> Parser<List<A>> sepBy(Parser<B> sep) {
        return sepBy1(sep).or(Parser.succeed(List.nil()));
    }

    default <B> Parser<List<A>> sepBy1(Parser<B> sep) {
        return this.flatMap(a -> 
            sep.then(this).many().map(as -> (List<A>) as.reverse().build(a).reverse())
        );
    }

    default Parser<A> peek() {
        return state -> parse(state).map(t -> Tuple.of(t.getA().orElse(null), state));
    }

    default <L, R> Parser<A> between(Parser<L> left, Parser<R> right) {
        return left.then(this).ignore(right);
    }

    default Parser<A> chainl1(Parser<BiFunction<A, A, A>> op) {
        return this.flatMap(a -> {
            Parser<Function<A, A>> rest = op.and(this).map(t -> 
                acc -> t.getA().orElse(null).apply(acc, t.getB().orElse(null))
            );
            return rest.many().map(fs -> fs.foldl(a, (acc, f) -> f.apply(acc)));
        });
    }

    default Parser<A> chainr1(Parser<BiFunction<A, A, A>> op) {
        return this.flatMap(a -> 
            op.and(lazy(() -> this.chainr1(op))).map(t -> t.getA().orElse(null).apply(a, t.getB().orElse(null)))
            .or(Parser.succeed(a))
        );
    }

    // --- Basic Parsers ---

    static <A> Parser<A> succeed(A value) {
        return state -> Either.right(Tuple.of(value, state));
    }

    static <A> Parser<A> fail(String message) {
        return state -> Either.left(new ParseError(state.position(), state.line(), state.column(), message));
    }

    static <A> Parser<A> lazy(java.util.function.Supplier<Parser<A>> p) {
        return state -> p.get().parse(state);
    }

    static Parser<Character> satisfy(Predicate<Character> pred, String expected) {
        return state -> {
            if (state.position() < state.source().length()) {
                char c = state.source().charAt(state.position());
                if (pred.test(c)) {
                    return Either.right(Tuple.of(c, state.advance(1)));
                }
            }
            return Either.left(new ParseError(state.position(), state.line(), state.column(), "Expected " + expected));
        };
    }

    static Parser<Character> character(char c) {
        return satisfy(ch -> ch == c, "'" + c + "'");
    }

    static Parser<String> string(String s) {
        return state -> {
            if (state.source().startsWith(s, state.position())) {
                return Either.right(Tuple.of(s, state.advance(s.length())));
            }
            return Either.left(new ParseError(state.position(), state.line(), state.column(), "Expected \"" + s + "\""));
        };
    }

    static Parser<Void> eof() {
        return state -> {
            if (state.position() >= state.source().length()) {
                return Either.right(Tuple.of(null, state));
            }
            return Either.left(new ParseError(state.position(), state.line(), state.column(), "Expected end of input"));
        };
    }

    static <A> Parser<A> choice(List<Parser<A>> ps) {
        return ps.foldl(Parser.<A>fail("No alternatives matched"), Parser::or);
    }

    static Parser<Character> any() {
        return satisfy(__ -> true, "any character");
    }

    static Parser<Character> digit() {
        return satisfy(Character::isDigit, "digit");
    }

    static Parser<Character> letter() {
        return satisfy(Character::isLetter, "letter");
    }

    static Parser<Character> whitespace() {
        return satisfy(Character::isWhitespace, "whitespace");
    }

    static Parser<List<Character>> spaces() {
        return whitespace().many();
    }

    default Parser<A> lexeme() {
        return this.ignore(spaces());
    }

    static Parser<String> identifier() {
        return letter().and(letter().or(digit()).many()).map(t -> 
            t.getA().orElse(null).toString() + t.getB().orElse(List.nil()).foldl("", (s, c) -> s + c)
        ).lexeme();
    }

    static Parser<Integer> integer() {
        return digit().many1().map(digits -> 
            Integer.parseInt(digits.foldl("", (s, c) -> s + c))
        ).lexeme();
    }
}
