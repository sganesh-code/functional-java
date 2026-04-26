package io.github.senthilganeshs.fj.parser;

/**
 * Represents a failure in parsing.
 */
public record ParseError(int position, String message) {
    @Override
    public String toString() {
        return "ParseError at position " + position + ": " + message;
    }
}
