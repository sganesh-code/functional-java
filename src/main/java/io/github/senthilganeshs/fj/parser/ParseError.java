package io.github.senthilganeshs.fj.parser;

/**
 * Represents a failure in the parsing process.
 */
public record ParseError(int position, int line, int column, String message) {
    @Override
    public String toString() {
        return "Parse error at line " + line + ", column " + column + " (pos " + position + "): " + message;
    }
}
