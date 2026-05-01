package io.github.senthilganeshs.fj.ds;

import io.github.senthilganeshs.fj.typeclass.Monoid;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class EffectTest {

    @Test
    public void testIO() {
        AtomicInteger counter = new AtomicInteger(0);
        IO<Integer> io = IO.of(() -> counter.incrementAndGet());
        
        Assert.assertEquals(counter.get(), 0); // Deferred
        Assert.assertEquals(io.unsafeRun(), Integer.valueOf(1));
        Assert.assertEquals(counter.get(), 1);
        
        IO<Integer> io2 = io.map(i -> i * 2);
        Assert.assertEquals(io2.unsafeRun(), Integer.valueOf(4));
        Assert.assertEquals(counter.get(), 2);
    }

    @Test
    public void testReader() {
        Reader<String, Integer> reader = Reader.<String>ask().map(String::length);
        Assert.assertEquals(reader.run().apply("Hello"), Integer.valueOf(5));
        
        Reader<String, String> reader2 = reader.flatMap(len -> Reader.pure("Length is " + len));
        Assert.assertEquals(reader2.run().apply("Test"), "Length is 4");
    }

    @Test
    public void testWriter() {
        Monoid<String> stringMonoid = new Monoid<>() {
            public String empty() { return ""; }
            public String combine(String a, String b) { return a + b; }
        };

        Writer<String, Integer> w1 = new Writer<>(10, "Init;");
        Writer<String, Integer> w2 = w1.flatMap(i -> new Writer<>(i + 5, "Added 5;"), stringMonoid);
        
        Assert.assertEquals(w2.value(), Integer.valueOf(15));
        Assert.assertEquals(w2.log(), "Init;Added 5;");
    }

    @Test
    public void testState() {
        State<Integer, String> state = State.<Integer>get()
            .flatMap(s -> State.<Integer>modify(i -> i + 1)
            .flatMap(__ -> State.pure("Old state was " + s)));
            
        Tuple<Integer, String> res = state.run().apply(10);
        Assert.assertEquals(res.getA().orElse(0), Integer.valueOf(11));
        Assert.assertEquals(res.getB().orElse(""), "Old state was 10");
    }

    @Test
    public void testTaskEither() {
        TaskEither<String, Integer> te1 = TaskEither.right(10);
        TaskEither<String, Integer> te2 = te1.flatMap(i -> TaskEither.right(i + 5));
        
        Assert.assertTrue(te2.task().run().isRight());
        Assert.assertEquals(te2.task().run().orElse(0), Integer.valueOf(15));

        TaskEither<String, Integer> te3 = te1.flatMap(i -> TaskEither.left("Fail"));
        Assert.assertTrue(te3.task().run().isLeft());
        Assert.assertEquals(te3.task().run().fromLeft(""), "Fail");
    }

    @Test
    public void testReaderTaskEither() {
        ReaderTaskEither<String, String, Integer> rte = ReaderTaskEither.<String, String>ask()
            .flatMap(env -> env.equals("secret") 
                ? ReaderTaskEither.right(100) 
                : ReaderTaskEither.left("Unauthorized"));

        Assert.assertEquals(rte.reader().run().apply("secret").task().run().orElse(0), Integer.valueOf(100));
        Assert.assertEquals(rte.reader().run().apply("wrong").task().run().fromLeft(""), "Unauthorized");
    }
}
