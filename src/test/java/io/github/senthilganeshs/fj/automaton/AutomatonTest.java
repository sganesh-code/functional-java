package io.github.senthilganeshs.fj.automaton;

import io.github.senthilganeshs.fj.ds.HashMap;
import io.github.senthilganeshs.fj.ds.List;
import io.github.senthilganeshs.fj.ds.Task;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class AutomatonTest {

    @Test
    public void testSimpleTransition() {
        // Simple counter machine
        Machine<Integer, String, String> machine = (state, input) -> 
            new Machine.Result<>(state + 1, List.nil());

        // Dummy persistence
        AtomicInteger savedState = new AtomicInteger(0);
        Repository<Task.µ, String, Integer> repo = new Repository<>() {
            @Override public Task<Integer> load(String key) { return Task.succeed(savedState.get()); }
            @Override public Task<Void> save(String key, Integer state) { 
                savedState.set(state); 
                return Task.succeed(null); 
            }
        };

        // No effects
        Interpreter<Task.µ, String, String> interpreter = cmd -> Task.succeed(List.nil());

        Automaton<Task.µ, String, Integer, String, String> automaton = 
            Automaton.ofTask(machine, interpreter, repo);

        Task<Integer> run = Task.narrowK(automaton.run("counter", "inc"));
        Integer result = run.run();

        Assert.assertEquals(result, Integer.valueOf(1));
        Assert.assertEquals(savedState.get(), 1);
    }

    @Test
    public void testEffectFeedbackLoop() {
        // Machine that emits a command on "start"
        Machine<String, String, String> machine = (state, input) -> {
            if (input.equals("start")) {
                return new Machine.Result<>("processing", List.of("fetch"));
            } else if (input.equals("data")) {
                return new Machine.Result<>("done", List.nil());
            }
            return new Machine.Result<>(state, List.nil());
        };

        // Interpreter that responds to "fetch" with "data"
        Interpreter<Task.µ, String, String> interpreter = cmd -> {
            if (cmd.equals("fetch")) {
                return Task.succeed(List.of("data"));
            }
            return Task.succeed(List.nil());
        };

        // Simple in-memory repo
        java.util.Map<String, String> db = new java.util.HashMap<>();
        db.put("job-1", "idle");
        
        Repository<Task.µ, String, String> repo = new Repository<>() {
            @Override public Task<String> load(String key) { return Task.succeed(db.get(key)); }
            @Override public Task<Void> save(String key, String state) { 
                db.put(key, state); 
                return Task.succeed(null); 
            }
        };

        Automaton<Task.µ, String, String, String, String> automaton = 
            Automaton.ofTask(machine, interpreter, repo);

        Task<String> run = Task.narrowK(automaton.run("job-1", "start"));
        String finalState = run.run();

        // Should have gone: idle -> start -> (processing + fetch) -> (done)
        Assert.assertEquals(finalState, "done");
        Assert.assertEquals(db.get("job-1"), "done");
    }

    @Test
    public void testCheckpointingOrder() {
        java.util.List<String> events = new ArrayList<>();

        Machine<Integer, String, String> machine = (state, input) -> 
            new Machine.Result<>(state + 1, List.of("cmd1"));

        Repository<Task.µ, String, Integer> repo = new Repository<>() {
            @Override public Task<Integer> load(String key) { return Task.succeed(0); }
            @Override public Task<Void> save(String key, Integer state) { 
                events.add("save-" + state);
                return Task.succeed(null); 
            }
        };

        Interpreter<Task.µ, String, String> interpreter = cmd -> {
            events.add("exec-" + cmd);
            return Task.succeed(List.nil());
        };

        Automaton<Task.µ, String, Integer, String, String> automaton = 
            Automaton.ofTask(machine, interpreter, repo);

        Task.narrowK(automaton.run("key", "input")).run();

        // Checkpointing: save must happen before execution
        Assert.assertEquals(events.size(), 2);
        Assert.assertEquals(events.get(0), "save-1");
        Assert.assertEquals(events.get(1), "exec-cmd1");
    }

    @Test
    public void testMultipleFeedbackInputs() {
        // Machine that splits an input into two parts
        Machine<List<String>, String, String> machine = (state, input) -> {
            if (input.equals("split")) {
                return new Machine.Result<>(state, List.of("emit-a", "emit-b"));
            }
            return new Machine.Result<>(List.from(state.build(input)), List.nil());
        };

        Interpreter<Task.µ, String, String> interpreter = cmd -> {
            if (cmd.equals("emit-a")) return Task.succeed(List.of("a"));
            if (cmd.equals("emit-b")) return Task.succeed(List.of("b"));
            return Task.succeed(List.nil());
        };

        AtomicReference<List<String>> db = new AtomicReference<>(List.nil());
        Repository<Task.µ, String, List<String>> repo = new Repository<>() {
            @Override public Task<List<String>> load(String key) { return Task.succeed(db.get()); }
            @Override public Task<Void> save(String key, List<String> state) { 
                db.set(state);
                return Task.succeed(null); 
            }
        };

        Automaton<Task.µ, String, List<String>, String, String> automaton = 
            Automaton.ofTask(machine, interpreter, repo);

        Task.narrowK(automaton.run("key", "split")).run();

        // Final state should contain both "a" and "b"
        Assert.assertTrue(db.get().contains("a"));
        Assert.assertTrue(db.get().contains("b"));
        Assert.assertEquals(db.get().length(), 2);
    }
}
