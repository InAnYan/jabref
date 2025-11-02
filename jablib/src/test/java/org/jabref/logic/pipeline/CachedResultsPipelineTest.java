package org.jabref.logic.pipeline;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.Executors; // Keep for mock TaskExecutor
import java.util.concurrent.TimeUnit;

import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.DelayTaskThrottler;
import org.jabref.logic.util.TaskExecutor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CachedResultsPipelineTest {

    private CachedResultsPipeline<TestIdentifiable, TestIdentifiable> pipeline;
    private TestResultsRepository resultsRepository;
    private TaskExecutor taskExecutor;

    private TaskExecutor createSynchronousTaskExecutor() {
        return new TaskExecutor() {
            @Override
            public <V> Future<V> execute(BackgroundTask<V> task) {
                try {
                    V result = task.call();
                    task.getOnSuccess().accept(result);
                    return CompletableFuture.completedFuture(result);
                } catch (Exception e) {
                    task.getOnException().accept(e);
                    return CompletableFuture.failedFuture(e);
                }
            }

            @Override
            public <V> Future<?> schedule(BackgroundTask<V> task, long delay, TimeUnit unit) {
                // For testing, run immediately
                return execute(task);
            }

            @Override
            public void shutdown() {
                // No-op for test
            }

            @Override
            public DelayTaskThrottler createThrottler(int delay) {
                return new DelayTaskThrottler(delay);
            }
        };
    }

    @BeforeEach
    void setUp() {
        taskExecutor = createSynchronousTaskExecutor();
        resultsRepository = new TestResultsRepository();
        pipeline = new CachedResultsPipeline<>(taskExecutor, resultsRepository);
    }

    @Test
    void testProcessingStoresResultInRepository() throws InterruptedException {
        TestPipelineStage stage = new TestPipelineStage("stage1");
        pipeline.addStage(stage);

        TestIdentifiable input = new TestIdentifiable("input", 0);
        pipeline.process(input);

        Thread.sleep(10);

        assertTrue(resultsRepository.isProcessed(input));
        Optional<TestIdentifiable> result = resultsRepository.getResult(input);
        assertTrue(result.isPresent());
        assertEquals("stage1", result.get().getName());
    }

    @Test
    void testRetrievesResultFromCacheInsteadOfProcessing() throws InterruptedException {
        TestIdentifiable input = new TestIdentifiable("input", 0);
        TestIdentifiable cachedResult = new TestIdentifiable("cached", 100);
        resultsRepository.storeResult(input, cachedResult);

        TestPipelineStage stage = new TestPipelineStage("stage1");
        pipeline.addStage(stage);

        PipelineObjectTracker<TestIdentifiable, TestIdentifiable> tracker = pipeline.process(input);

        Thread.sleep(10);

        assertTrue(tracker.isFinished());
        Optional<TestIdentifiable> result = tracker.getResult();
        assertTrue(result.isPresent());
        assertEquals("cached", result.get().getName());
        assertEquals(100, result.get().getValue());
    }

    static class TestIdentifiable implements Identifiable {
        private final String name;
        private final int value;

        TestIdentifiable(String name, int value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String getId() {
            return name + "_" + value;
        }

        String getName() {
            return name;
        }

        int getValue() {
            return value;
        }
    }

    static class TestPipelineStage extends PipelineStage<TestIdentifiable, TestIdentifiable> {
        private final String stageName;

        TestPipelineStage(String stageName) {
            super("Processing " + stageName);
            this.stageName = stageName;
        }

        @Override
        public TestIdentifiable process(TestIdentifiable obj) {
            return new TestIdentifiable(stageName, obj.getValue() + 1);
        }
    }

    static class TestResultsRepository implements ResultsRepository<TestIdentifiable> {
        private final Map<String, TestIdentifiable> cache = new HashMap<>();

        @Override
        public boolean isProcessed(Identifiable obj) {
            return cache.containsKey(obj.getId());
        }

        @Override
        public Optional<TestIdentifiable> getResult(Identifiable obj) {
            return Optional.ofNullable(cache.get(obj.getId()));
        }

        @Override
        public void storeResult(Identifiable obj, TestIdentifiable result) {
            cache.put(obj.getId(), result);
        }
    }
}
