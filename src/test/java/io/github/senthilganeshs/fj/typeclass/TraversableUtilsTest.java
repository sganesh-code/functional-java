package io.github.senthilganeshs.fj.typeclass;

import io.github.senthilganeshs.fj.hkt.Higher;
import io.github.senthilganeshs.fj.ds.List;
import io.github.senthilganeshs.fj.ds.Maybe;
import io.github.senthilganeshs.fj.ds.Collection;
import io.github.senthilganeshs.fj.ds.Reader;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

import java.util.function.BiFunction;
import java.util.function.Function;

public class TraversableUtilsTest {

    static final Traversable<Collection.µ> collectionTraversable = new Traversable<Collection.µ>() {
        @Override
        public <A, B> Higher<Collection.µ, B> map(Function<A, B> fn, Higher<Collection.µ, A> fa) {
            return Collection.narrowK(fa).map(fn);
        }

        @Override
        public <A, B> B foldl(BiFunction<B, A, B> f, B seed, Higher<Collection.µ, A> fa) {
            return Collection.narrowK(fa).foldl(seed, f);
        }

        @Override
        public <A, B> B foldr(BiFunction<A, B, B> f, B seed, Higher<Collection.µ, A> fa) {
            return Collection.narrowK(fa).foldr(seed, f);
        }

        @Override
        public <G, A, B> Higher<G, Higher<Collection.µ, B>> traverse(Applicative<G> app, Function<A, Higher<G, B>> fn, Higher<Collection.µ, A> fa) {
            Collection<A> src = Collection.narrowK(fa);
            Higher<G, Collection<B>> result = src.foldl(app.pure(src.empty()), (acc, a) -> 
                app.liftA2((col, b) -> col.build(b), acc, fn.apply(a))
            );
            return (Higher<G, Higher<Collection.µ, B>>) (Higher) result;
        }
    };

    static final Profunctor<Reader.µ> readerProfunctor = new Profunctor<Reader.µ>() {
        @Override
        public <A, B, C, D> Higher<Higher<Reader.µ, A>, D> dimap(Function<A, B> f, Function<C, D> g, Higher<Higher<Reader.µ, B>, C> pbc) {
            Reader<B, C> readerBC = Reader.narrowK(pbc);
            return new Reader<>(a -> g.apply(readerBC.run(f.apply(a))));
        }
    };

    @Test
    public void testTraverse() {
        List<Integer> list = List.of(1, 2, 3);
        Higher<Maybe.µ, Higher<Collection.µ, Integer>> res = Traversable.traverse(
            collectionTraversable, 
            Maybe.monad, 
            x -> x > 0 ? Maybe.some(x * 2) : Maybe.nothing(), 
            list
        );
        Maybe<List<Integer>> maybeList = Maybe.from(((Maybe<Higher<Collection.µ, Integer>>) res).map(h -> List.from(Collection.narrowK(h))));
        assertEquals(maybeList.orElse(List.nil()).toString(), "[2,4,6]");

        Higher<Maybe.µ, Higher<Collection.µ, Integer>> res2 = Traversable.traverse(
            collectionTraversable, 
            Maybe.monad, 
            x -> x > 2 ? Maybe.some(x) : Maybe.nothing(), 
            list
        );
        assertTrue(((Maybe<Higher<Collection.µ, Integer>>) res2).isNothing());
    }

    @Test
    public void testSequence() {
        List<Maybe<Integer>> list = List.of(Maybe.some(1), Maybe.some(2));
        Higher<Maybe.µ, Higher<Collection.µ, Integer>> res = Traversable.sequence(
            collectionTraversable, 
            Maybe.monad, 
            (Higher) list
        );
        Maybe<List<Integer>> maybeList = Maybe.from(((Maybe<Higher<Collection.µ, Integer>>) res).map(h -> List.from(Collection.narrowK(h))));
        assertEquals(maybeList.orElse(List.nil()).toString(), "[1,2]");
    }

    @Test
    public void testDimap() {
        Reader<Integer, String> reader = new Reader<>(Object::toString);
        // dimap: (String -> Integer) -> (String -> Integer) -> Reader<Integer, String> -> Reader<String, Integer>
        Higher<Higher<Reader.µ, String>, Integer> res = Profunctor.dimap(
            readerProfunctor, 
            Integer::parseInt, 
            Integer::parseInt, 
            reader
        );
        Reader<String, Integer> readerRes = Reader.narrowK(res);
        assertEquals(readerRes.run("123"), (Integer) 123);
    }
}
