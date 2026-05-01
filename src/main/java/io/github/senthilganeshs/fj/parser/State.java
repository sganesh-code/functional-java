package io.github.senthilganeshs.fj.parser;

import io.github.senthilganeshs.fj.ds.Maybe;

/**
 * Immutable state tracking the parsing position, including line and column.
 */
public record State(String source, int position, int line, int column) {
    
    public State(String source, int position) {
        this(source, position, 1, 1);
    }

    public Maybe<Character> current() {
        if (isEOF()) return Maybe.nothing();
        return Maybe.some(source.charAt(position));
    }

    public State advance(int chars) {
        int newPos = position;
        int newLine = line;
        int newCol = column;

        for (int i = 0; i < chars && newPos < source.length(); i++) {
            char c = source.charAt(newPos);
            newPos++;
            if (c == '\n') {
                newLine++;
                newCol = 1;
            } else {
                newCol++;
            }
        }
        return new State(source, newPos, newLine, newCol);
    }

    public boolean isEOF() {
        return position >= source.length();
    }

    public String remaining() {
        return source.substring(position);
    }
}
