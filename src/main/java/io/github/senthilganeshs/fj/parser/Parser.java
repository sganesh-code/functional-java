package io.github.senthilganeshs.fj.parser;

import io.github.senthilganeshs.fj.ds.Either;
import io.github.senthilganeshs.fj.ds.List;
import io.github.senthilganeshs.fj.ds.Maybe;
import io.github.senthilganeshs.fj.ds.Tuple;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A functional parser that transforms a State into an Either a ParseError or a successful value with a new State.
 * @param <A> The type of value produced by the parser.
 */
@FunctionalInterface
public interface Parser<A> {

    Either<ParseError, Tuple<A, State>> parse(State state);

    default Either<ParseError, A> parse(String input) {
        return parse(new State(input, 0)).map(t -> t.getA().orElse(null));
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
            if (res.isRight()) return res;
            return other.parse(state);
        };
    }

    default <B> Parser<Tuple<A, B>> and(Parser<B> other) {
        return this.flatMap(a -> other.map(b -> Tuple.of(a, b)));
    }

    default <B> Parser<A> ignore(Parser<B> other) {
        return this.flatMap(a -> other.map(b -> a));
    }

    default <B> Parser<B> then(Parser<B> other) {
        return this.flatMap(a -> other);
    }

    default Parser<List<A>> many() {
        return state -> {
            List<A> results = List.nil();
            State currentState = state;
            while (true) {
                Either<ParseError, Tuple<A, State>> res = parse(currentState);
                if (res.isLeft()) break;
                Tuple<A, State> t = res.orElse(null);
                results = results.build(t.getA().orElse(null));
                currentState = t.getB().orElse(null);
            }
            return Either.right(Tuple.of(results, currentState));
        };
    }

    default Parser<List<A>> many1() {
        return this.flatMap(first -> many().map(rest -> List.from(List.of(first).concat(rest))));
    }

    default <B> Parser<List<A>> sepBy(Parser<B> sep) {
        return sepBy1(sep).or(succeed(List.nil()));
    }

    default <B> Parser<List<A>> sepBy1(Parser<B> sep) {
        return this.flatMap(first -> 
            sep.then(this).many().map(rest -> List.from(List.of(first).concat(rest)))
        );
    }

    default Parser<Maybe<A>> optional() {
        return state -> {
            Either<ParseError, Tuple<A, State>> res = parse(state);
            if (res.isRight()) {
                Tuple<A, State> t = res.orElse(null);
                return Either.right(Tuple.of(Maybe.some(t.getA().orElse(null)), t.getB().orElse(null)));
            }
            return Either.right(Tuple.of(Maybe.nothing(), state));
        };
    }

    default Parser<A> peek() {
        return state -> parse(state).map(t -> Tuple.of(t.getA().orElse(null), state));
    }

    default <L, R> Parser<A> between(Parser<L> left, Parser<R> right) {
        return left.then(this).ignore(right);
    }

    // --- Basic Parsers ---

    static <A> Parser<A> lazy(java.util.function.Supplier<Parser<A>> supplier) {
        return state -> supplier.get().parse(state);
    }

    static <A> Parser<A> succeed(A value) {
        return state -> Either.right(Tuple.of(value, state));
    }

    static <A> Parser<A> fail(String message) {
        return state -> Either.left(new ParseError(state.position(), message));
    }

    static Parser<Character> satisfy(Predicate<Character> pred, String expected) {
        return state -> state.current()
            .map(c -> pred.test(c) 
                ? Either.<ParseError, Tuple<Character, State>>right(Tuple.of(c, state.advance(1)))
                : Either.<ParseError, Tuple<Character, State>>left(new ParseError(state.position(), "Expected " + expected + " but found " + c)))
            .orElse(Either.left(new ParseError(state.position(), "Expected " + expected + " but reached EOF")));
    }

    static Parser<Character> character(char c) {
        return satisfy(ch -> ch == c, "'" + c + "'");
    }

    static Parser<String> string(String s) {
        return state -> {
            if (state.source().startsWith(s, state.position())) {
                return Either.right(Tuple.of(s, state.advance(s.length())));
            }
            return Either.left(new ParseError(state.position(), "Expected \"" + s + "\""));
        };
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

    static Parser<Void> eof() {
        return state -> state.isEOF() 
            ? Either.right(Tuple.of(null, state))
            : Either.left(new ParseError(state.position(), "Expected EOF"));
    }

    static <A> Parser<A> choice(List<Parser<A>> parsers) {
        return state -> parsers.foldl(Either.<ParseError, Tuple<A, State>>left(new ParseError(state.position(), "No choice matched")),
            (acc, p) -> acc.isRight() ? acc : p.parse(state));
    }
}
