package io.github.senthilganeshs.fj.automaton;

import io.github.senthilganeshs.fj.ds.List;
import io.github.senthilganeshs.fj.ds.Task;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicReference;

public class AdviceAgentIntegrationTest {

    // --- Data Models ---

    sealed interface AdviceState {
        record Idle() implements AdviceState {}
        record Thinking(String prompt) implements AdviceState {}
        record Responding(String thought) implements AdviceState {}
        record Done(String response) implements AdviceState {}
    }

    sealed interface DilMessage {
        record AdviceRequested(String prompt) implements DilMessage {}
        record ThoughtProduced(String thought) implements DilMessage {}
        record ResponseGenerated(String response) implements DilMessage { }
    }

    sealed interface DilCommand {
        record GenerateThought(String prompt) implements DilCommand {}
        record GenerateResponse(String thought) implements DilCommand {}
    }

    // --- Components ---

    static class AdviceSessionPipeline implements Machine<AdviceState, DilMessage, DilCommand> {
        @Override
        public Result<AdviceState, DilCommand> transition(AdviceState state, DilMessage input) {
            return switch (input) {
                case DilMessage.AdviceRequested req -> 
                    new Result<>(new AdviceState.Thinking(req.prompt()), List.of(new DilCommand.GenerateThought(req.prompt())));
                
                case DilMessage.ThoughtProduced tp -> 
                    new Result<>(new AdviceState.Responding(tp.thought()), List.of(new DilCommand.GenerateResponse(tp.thought())));
                
                case DilMessage.ResponseGenerated rg -> 
                    new Result<>(new AdviceState.Done(rg.response()), List.nil());
            };
        }
    }

    static class AdviceWorker implements Interpreter<Task.µ, DilCommand, DilMessage> {
        @Override
        public Task<List<DilMessage>> execute(DilCommand command) {
            return switch (command) {
                case DilCommand.GenerateThought gt -> 
                    Task.succeed(List.of(new DilMessage.ThoughtProduced("Thought about: " + gt.prompt())));
                
                case DilCommand.GenerateResponse gr -> 
                    Task.succeed(List.of(new DilMessage.ResponseGenerated("Response to: " + gr.thought())));
            };
        }
    }

    static class AdviceSessionStore implements Repository<Task.µ, String, AdviceState> {
        private final AtomicReference<AdviceState> db = new AtomicReference<>(new AdviceState.Idle());

        @Override
        public Task<AdviceState> load(String key) {
            return Task.succeed(db.get());
        }

        @Override
        public Task<Void> save(String key, AdviceState state) {
            db.set(state);
            return Task.succeed(null);
        }

        public AdviceState getCurrentState() {
            return db.get();
        }
    }

    // --- Test ---

    @Test
    public void testAdviceAgentFlow() {
        AdviceSessionPipeline logic = new AdviceSessionPipeline();
        AdviceWorker worker = new AdviceWorker();
        AdviceSessionStore repo = new AdviceSessionStore();

        Automaton<Task.µ, String, AdviceState, DilMessage, DilCommand> engine = 
            Automaton.ofTask(logic, worker, repo);

        Task<AdviceState> run = Task.narrowK(engine.run("session-1", new DilMessage.AdviceRequested("How to use FJ?")));
        AdviceState finalState = run.run();

        // Verify terminal state
        Assert.assertTrue(finalState instanceof AdviceState.Done);
        AdviceState.Done done = (AdviceState.Done) finalState;
        Assert.assertEquals(done.response(), "Response to: Thought about: How to use FJ?");

        // Verify persistence
        Assert.assertEquals(repo.getCurrentState(), finalState);
    }
}
