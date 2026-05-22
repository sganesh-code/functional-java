module io.github.senthilganeshs.fj {
    requires transitive reactor.core;
    requires org.reactivestreams;

    exports io.github.senthilganeshs.fj.codec;
    exports io.github.senthilganeshs.fj.ds;
    exports io.github.senthilganeshs.fj.hkt;
    exports io.github.senthilganeshs.fj.optic;
    exports io.github.senthilganeshs.fj.reactor;
    exports io.github.senthilganeshs.fj.parser;
    exports io.github.senthilganeshs.fj.stream;
    exports io.github.senthilganeshs.fj.test;
    exports io.github.senthilganeshs.fj.typeclass;
}
