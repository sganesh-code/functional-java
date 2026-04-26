package io.github.senthilganeshs.fj.parser;

import io.github.senthilganeshs.fj.ds.Maybe;

/**
 * Immutable state tracking the parsing position.
 */
public record State(String source, int position) {
    
    public Maybe<Character> current() {
        if (isEOF()) return Maybe.nothing();
        return Maybe.some(source.charAt(position));
    }

    public State advance(int chars) {
        return new State(source, position + chars);
    }

    public boolean isEOF() {
        return position >= source.length();
    }

    public String remaining() {
        return source.substring(position);
    }
}
