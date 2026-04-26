package io.github.senthilganeshs.fj.parser;

import io.github.senthilganeshs.fj.ds.Either;
import io.github.senthilganeshs.fj.ds.List;
import io.github.senthilganeshs.fj.ds.Maybe;
import io.github.senthilganeshs.fj.ds.Tuple;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ParserTest {

    @Test
    public void testBasicParsers() {
        Assert.assertEquals(Parser.character('a').parse("abc").orElse(null), (Character)'a');
        Assert.assertTrue(Parser.character('a').parse("bcd").isLeft());
        
        Assert.assertEquals(Parser.string("hello").parse("hello world").orElse(null), "hello");
        Assert.assertTrue(Parser.string("hello").parse("hi").isLeft());
        
        Assert.assertEquals(Parser.digit().parse("123").orElse(null), (Character)'1');
        Assert.assertTrue(Parser.digit().parse("abc").isLeft());
        
        Assert.assertEquals(Parser.letter().parse("X12").orElse(null), (Character)'X');
        Assert.assertTrue(Parser.letter().parse("123").isLeft());
        
        Assert.assertEquals(Parser.whitespace().parse(" \t").orElse(null), (Character)' ');
        Assert.assertTrue(Parser.whitespace().parse("a").isLeft());
        
        Assert.assertTrue(Parser.eof().parse("").isRight());
        Assert.assertTrue(Parser.eof().parse("a").isLeft());
    }

    @Test
    public void testCombinators() {
        // map
        Parser<Integer> digitInt = Parser.digit().map(c -> Character.getNumericValue(c));
        Assert.assertEquals(digitInt.parse("5").orElse(null), (Integer)5);

        // and
        Parser<Tuple<Character, Character>> pair = Parser.character('a').and(Parser.character('b'));
        Tuple<Character, Character> res = pair.parse("abc").orElse(null);
        Assert.assertEquals(res.getA().orElse(null), (Character)'a');
        Assert.assertEquals(res.getB().orElse(null), (Character)'b');

        // or
        Parser<Character> either = Parser.character('a').or(Parser.character('b'));
        Assert.assertEquals(either.parse("abc").orElse(null), (Character)'a');
        Assert.assertEquals(either.parse("bcd").orElse(null), (Character)'b');

        // then / ignore
        Assert.assertEquals(Parser.character('a').then(Parser.character('b')).parse("abc").orElse(null), (Character)'b');
        Assert.assertEquals(Parser.character('a').ignore(Parser.character('b')).parse("abc").orElse(null), (Character)'a');
    }

    @Test
    public void testRepetition() {
        // many
        List<Character> as = Parser.character('a').many().parse("aaab").orElse(null);
        Assert.assertEquals(as.length(), 3);
        
        // many1
        Assert.assertTrue(Parser.character('a').many1().parse("bbb").isLeft());
        Assert.assertEquals(Parser.character('a').many1().parse("aabb").orElse(null).length(), 2);

        // sepBy
        Parser<List<Character>> csv = Parser.letter().sepBy(Parser.character(','));
        List<Character> letters = csv.parse("a,b,c").orElse(null);
        Assert.assertEquals(letters.length(), 3);
        Assert.assertEquals(letters.drop(0).headMaybe().orElse(null), (Character)'a');
        Assert.assertEquals(letters.drop(1).headMaybe().orElse(null), (Character)'b');
        Assert.assertEquals(letters.drop(2).headMaybe().orElse(null), (Character)'c');

        Assert.assertEquals(csv.parse("").orElse(null).length(), 0);
    }

    @Test
    public void testOptionalAndPeek() {
        Parser<Maybe<Character>> opt = Parser.character('a').optional();
        Assert.assertEquals(opt.parse("abc").orElse(null), Maybe.some('a'));
        Assert.assertEquals(opt.parse("bcd").orElse(null), Maybe.nothing());

        Parser<Character> peek = Parser.character('a').peek();
        State s = new State("abc", 0);
        Either<ParseError, Tuple<Character, State>> res = peek.parse(s);
        Assert.assertTrue(res.isRight());
        Assert.assertEquals(res.orElse(null).getA().orElse(null), (Character)'a');
        Assert.assertEquals(res.orElse(null).getB().orElse(null).position(), 0); // Position not advanced
    }

    @Test
    public void testParseErrorPosition() {
        Parser<String> p = Parser.string("abc").then(Parser.string("def"));
        Either<ParseError, String> res = p.parse("abcghi");
        Assert.assertTrue(res.isLeft());
        Assert.assertEquals(res.fromLeft(null).position(), 3);
        Assert.assertTrue(res.fromLeft(null).message().contains("Expected \"def\""));
    }

    @Test
    public void testChoice() {
        Parser<String> p = Parser.choice(List.of(Parser.string("apple"), Parser.string("banana"), Parser.string("cherry")));
        Assert.assertEquals(p.parse("banana cake").orElse(null), "banana");
        Assert.assertTrue(p.parse("dog").isLeft());
    }
}
