package org.jabref.logic.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors; // Keep for mock TaskExecutor
import java.util.concurrent.Future; // Needed for TaskExecutor

import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.jabref.logic.util.DelayTaskThrottler;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PipelineTest {

    private Pipeline<TestIdentifiable, TestIdentifiable> pipeline;
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
        // Use a simple TaskExecutor that runs tasks synchronously for testing
        taskExecutor = createSynchronousTaskExecutor();
        pipeline = new Pipeline<>(taskExecutor);
    }

    @Test
    void testCreatePipeline() {
        assertNotNull(pipeline);
    }

    @Test
    void testAddStage() throws InterruptedException {
        TestPipelineStage stage = new TestPipelineStage("stage1");
        pipeline.addStage(stage);
        // Verify stage was added by processing
        TestIdentifiable input = new TestIdentifiable("input", 0);
        PipelineObjectTracker<TestIdentifiable, TestIdentifiable> tracker = pipeline.process(input);
        
        // Wait a bit for processing (since we're using synchronous executor, it should be immediate)
        Thread.sleep(10);
        
        assertTrue(tracker.isFinished());
        assertNotNull(tracker.getResult().orElse(null));
        assertEquals("stage1", tracker.getResult().get().getName());
    }

    @Test
    void testProcessThroughSingleStage() throws InterruptedException {
        TestPipelineStage stage = new TestPipelineStage("stage1");
        pipeline.addStage(stage);

        TestIdentifiable input = new TestIdentifiable("input", 0);
        PipelineObjectTracker<TestIdentifiable, TestIdentifiable> tracker = pipeline.process(input);
        
        // Wait for processing to complete
        Thread.sleep(10);

        assertTrue(tracker.isFinished());
        assertFalse(tracker.isProcessing());
        assertFalse(tracker.isError());
        TestIdentifiable result = tracker.getResult().orElse(null);
        assertNotNull(result);
        assertEquals("stage1", result.getName());
        assertEquals(1, result.getValue());
    }

    @Test
    void testProcessThroughMultipleStages() throws InterruptedException {
        TestPipelineStage stage1 = new TestPipelineStage("stage1");
        TestPipelineStage stage2 = new TestPipelineStage("stage2");
        TestPipelineStage stage3 = new TestPipelineStage("stage3");

        pipeline.addStage(stage1);
        pipeline.addStage(stage2);
        pipeline.addStage(stage3);

        TestIdentifiable input = new TestIdentifiable("input", 0);
        PipelineObjectTracker<TestIdentifiable, TestIdentifiable> tracker = pipeline.process(input);
        
        // Wait for processing to complete
        Thread.sleep(10);

        assertTrue(tracker.isFinished());
        TestIdentifiable result = tracker.getResult().orElse(null);
        assertNotNull(result);
        // Should have been processed through all three stages
        assertEquals("stage3", result.getName());
        assertEquals(3, result.getValue());
    }

    @Test
    void testPipelineStagesExecuteInOrder() throws InterruptedException {
        List<String> executionOrder = new ArrayList<>();

        TestPipelineStage stage1 = new TestPipelineStage("stage1") {
            @Override
            public TestIdentifiable process(TestIdentifiable obj) {
                executionOrder.add("stage1");
                return super.process(obj);
            }
        };

        TestPipelineStage stage2 = new TestPipelineStage("stage2") {
            @Override
            public TestIdentifiable process(TestIdentifiable obj) {
                executionOrder.add("stage2");
                return super.process(obj);
            }
        };

        TestPipelineStage stage3 = new TestPipelineStage("stage3") {
            @Override
            public TestIdentifiable process(TestIdentifiable obj) {
                executionOrder.add("stage3");
                return super.process(obj);
            }
        };

        pipeline.addStage(stage1);
        pipeline.addStage(stage2);
        pipeline.addStage(stage3);

        TestIdentifiable input = new TestIdentifiable("input", 0);
        PipelineObjectTracker<TestIdentifiable, TestIdentifiable> tracker = pipeline.process(input);
        
        // Wait for processing to complete
        Thread.sleep(10);

        assertEquals(3, executionOrder.size());
        assertEquals("stage1", executionOrder.get(0));
        assertEquals("stage2", executionOrder.get(1));
        assertEquals("stage3", executionOrder.get(2));
    }

    @Test
    void testEmptyPipelineReturnsSameObject() throws InterruptedException {
        TestIdentifiable input = new TestIdentifiable("input", 5);
        PipelineObjectTracker<TestIdentifiable, TestIdentifiable> tracker = pipeline.process(input);
        
        // Wait for processing to complete
        Thread.sleep(10);

        // When no stages, should return the same object
        assertTrue(tracker.isFinished());
        TestIdentifiable result = tracker.getResult().orElse(null);
        assertNotNull(result);
        assertEquals(input.getId(), result.getId());
    }

    @Test
    void testPipelineWithTransformingStages() throws InterruptedException {
        // Stage that modifies the value
        TestPipelineStage incrementStage = new TestPipelineStage("increment") {
            @Override
            public TestIdentifiable process(TestIdentifiable obj) {
                return new TestIdentifiable(obj.getName(), obj.getValue() + 10);
            }
        };

        // Stage that modifies the name
        TestPipelineStage renameStage = new TestPipelineStage("rename") {
            @Override
            public TestIdentifiable process(TestIdentifiable obj) {
                return new TestIdentifiable("renamed_" + obj.getName(), obj.getValue());
            }
        };

        pipeline.addStage(incrementStage);
        pipeline.addStage(renameStage);

        TestIdentifiable input = new TestIdentifiable("original", 5);
        PipelineObjectTracker<TestIdentifiable, TestIdentifiable> tracker = pipeline.process(input);
        
        // Wait for processing to complete
        Thread.sleep(10);

        assertTrue(tracker.isFinished());
        TestIdentifiable result = tracker.getResult().orElse(null);
        assertNotNull(result);
        assertEquals("renamed_original", result.getName());
        assertEquals(15, result.getValue()); // 5 + 10
    }

    @Test
    void testPipelineWithMultipleObjects() throws InterruptedException {
        TestPipelineStage stage1 = new TestPipelineStage("stage1");
        TestPipelineStage stage2 = new TestPipelineStage("stage2");
        pipeline.addStage(stage1);
        pipeline.addStage(stage2);

        TestIdentifiable obj1 = new TestIdentifiable("obj1", 0);
        TestIdentifiable obj2 = new TestIdentifiable("obj2", 10);
        TestIdentifiable obj3 = new TestIdentifiable("obj3", 20);

        PipelineObjectTracker<TestIdentifiable, TestIdentifiable> tracker1 = pipeline.process(obj1);
        PipelineObjectTracker<TestIdentifiable, TestIdentifiable> tracker2 = pipeline.process(obj2);
        PipelineObjectTracker<TestIdentifiable, TestIdentifiable> tracker3 = pipeline.process(obj3);
        
        // Wait for processing to complete
        Thread.sleep(10);

        assertTrue(tracker1.isFinished());
        assertTrue(tracker2.isFinished());
        assertTrue(tracker3.isFinished());
        
        TestIdentifiable result1 = tracker1.getResult().orElse(null);
        TestIdentifiable result2 = tracker2.getResult().orElse(null);
        TestIdentifiable result3 = tracker3.getResult().orElse(null);
        
        assertEquals("stage2", result1.getName());
        assertEquals(2, result1.getValue());
        assertEquals("stage2", result2.getName());
        assertEquals(12, result2.getValue());
        assertEquals("stage2", result3.getName());
        assertEquals(22, result3.getValue());
    }

    @Test
    void testTypeCheckWithCompatibleTypes() {
        // Test that compatible types (same types) work
        TestPipelineStage stage1 = new TestPipelineStage("stage1");
        TestPipelineStage stage2 = new TestPipelineStage("stage2");
        
        pipeline.addStage(stage1);
        // Should not throw - both stages have TestIdentifiable -> TestIdentifiable
        pipeline.addStage(stage2);
    }

    @Test
    void testTypeCheckWithIncompatibleTypes() {
        // Create a stage that accepts a different type
        TestPipelineStage stage1 = new TestPipelineStage("stage1");
        DifferentTypeStage stage2 = new DifferentTypeStage("stage2");
        
        pipeline.addStage(stage1);
        // Should throw - stage1 returns TestIdentifiable, but stage2 accepts DifferentIdentifiable
        assertThrows(IllegalArgumentException.class, () -> pipeline.addStage(stage2));
    }

    @Test
    void testPipelineErrorHandling() throws InterruptedException {
        // Stage that throws an exception
        TestPipelineStage errorStage = new TestPipelineStage("error") {
            @Override
            public TestIdentifiable process(TestIdentifiable obj) {
                throw new RuntimeException("Test error");
            }
        };

        pipeline.addStage(errorStage);

        TestIdentifiable input = new TestIdentifiable("input", 0);
        PipelineObjectTracker<TestIdentifiable, TestIdentifiable> tracker = pipeline.process(input);
        
        // Wait for processing to complete
        Thread.sleep(10);

        assertTrue(tracker.isError());
        assertFalse(tracker.isProcessing());
        assertFalse(tracker.isFinished());
        assertTrue(tracker.getError().isPresent());
        Exception error = tracker.getError().get();
        assertTrue(error.getMessage().contains("Test error"));
        assertTrue(error instanceof RuntimeException);
    }

    @Test
    void testPipelineErrorHandlingInMiddleStage() throws InterruptedException {
        // Multiple stages where error occurs in the middle
        TestPipelineStage stage1 = new TestPipelineStage("stage1");
        TestPipelineStage errorStage = new TestPipelineStage("error") {
            @Override
            public TestIdentifiable process(TestIdentifiable obj) {
                throw new IllegalArgumentException("Middle stage error");
            }
        };
        TestPipelineStage stage3 = new TestPipelineStage("stage3"); // Should never execute

        pipeline.addStage(stage1);
        pipeline.addStage(errorStage);
        pipeline.addStage(stage3);

        TestIdentifiable input = new TestIdentifiable("input", 0);
        PipelineObjectTracker<TestIdentifiable, TestIdentifiable> tracker = pipeline.process(input);
        
        // Wait for processing to complete
        Thread.sleep(10);

        assertTrue(tracker.isError());
        assertFalse(tracker.isProcessing());
        assertFalse(tracker.isFinished());
        assertTrue(tracker.getError().isPresent());
        Exception error = tracker.getError().get();
        assertNotNull(error);
        assertTrue(error instanceof IllegalArgumentException || 
                   (error.getMessage() != null && error.getMessage().contains("Middle stage error")));
        // Verify stage3 was never executed (result should not have stage3 name)
        assertFalse(tracker.getResult().isPresent());
    }

    @Test
    void testPipelineErrorHandlingDifferentExceptionTypes() throws InterruptedException {
        // Test different exception types
        TestPipelineStage illegalArgStage = new TestPipelineStage("error") {
            @Override
            public TestIdentifiable process(TestIdentifiable obj) {
                throw new IllegalStateException("Illegal state error");
            }
        };

        pipeline.addStage(illegalArgStage);

        TestIdentifiable input = new TestIdentifiable("input", 0);
        PipelineObjectTracker<TestIdentifiable, TestIdentifiable> tracker = pipeline.process(input);
        
        Thread.sleep(10);

        assertTrue(tracker.isError());
        assertTrue(tracker.getError().isPresent());
        Exception error = tracker.getError().get();
        assertNotNull(error);
        // The exception should be preserved
        assertTrue(error instanceof IllegalStateException || 
                   (error.getMessage() != null && error.getMessage().contains("Illegal state")));
    }

    @Test
    void testIdempotencyWhileProcessing() throws InterruptedException {
        // Use async executor for this test to avoid timing issues
        TaskExecutor asyncTaskExecutor = new TaskExecutor() {
            @Override
            public <V> Future<V> execute(BackgroundTask<V> task) {
                // Simulate async execution
                return Executors.newSingleThreadExecutor().submit(() -> {
                    V result = null;
                    try {
                        result = task.call();
                        task.getOnSuccess().accept(result);
                    } catch (Exception e) {
                        task.getOnException().accept(e);
                    }
                    return result;
                });
            }

            @Override
            public <V> Future<?> schedule(BackgroundTask<V> task, long delay, TimeUnit unit) {
                return Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                    execute(task);
                    return null;
                }, delay, unit);
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
        Pipeline<TestIdentifiable, TestIdentifiable> asyncPipeline = new Pipeline<>(asyncTaskExecutor);

        // Create a slow stage to ensure we can test while processing
        TestPipelineStage slowStage = new TestPipelineStage("slow") {
            @Override
            public TestIdentifiable process(TestIdentifiable obj) {
                try {
                    Thread.sleep(100); // Make processing slow enough to test
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return super.process(obj);
            }
        };
        asyncPipeline.addStage(slowStage);

        TestIdentifiable obj = new TestIdentifiable("test", 1);

        // First call - should start processing
        PipelineObjectTracker<TestIdentifiable, TestIdentifiable> tracker1 = asyncPipeline.process(obj);
        // State should be PROCESSING immediately
        assertTrue(tracker1.isProcessing(), "Tracker should be processing immediately after process() call");

        // Second call with same object (same ID) - should return same tracker
        PipelineObjectTracker<TestIdentifiable, TestIdentifiable> tracker2 = asyncPipeline.process(obj);
        assertSame(tracker1, tracker2, "Same object should return same tracker while processing");
        assertTrue(tracker2.isProcessing(), "Second tracker should also be processing");

        // Wait for processing to complete
        Thread.sleep(150);

        assertTrue(tracker1.isFinished(), "Tracker1 should be finished");
        assertTrue(tracker2.isFinished(), "Tracker2 should be finished");
    }

    @Test
    void testObservableTracker() throws InterruptedException {
        TaskExecutor asyncTaskExecutor = new TaskExecutor() {
            @Override
            public <V> Future<V> execute(BackgroundTask<V> task) {
                return Executors.newSingleThreadExecutor().submit(() -> {
                    V result = null;
                    try {
                        result = task.call();
                        task.getOnSuccess().accept(result);
                    } catch (Exception e) {
                        task.getOnException().accept(e);
                    }
                    return result;
                });
            }

            @Override
            public <V> Future<?> schedule(BackgroundTask<V> task, long delay, TimeUnit unit) {
                return Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                    execute(task);
                    return null;
                }, delay, unit);
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
        Pipeline<TestIdentifiable, TestIdentifiable> asyncPipeline = new Pipeline<>(asyncTaskExecutor);
        TestPipelineStage stage = new TestPipelineStage("stage1");
        asyncPipeline.addStage(stage);

        TestIdentifiable input = new TestIdentifiable("input", 0);
        PipelineObjectTracker<TestIdentifiable, TestIdentifiable> tracker = asyncPipeline.process(input);

        Thread.sleep(50);

        assertTrue(tracker.isFinished());
        assertEquals(PipelineObjectTracker.State.FINISHED, tracker.getState());
    }

    @Test
    void testIdempotencyAfterFinished() throws InterruptedException {
        // Use async executor for this test to avoid timing issues
        TaskExecutor asyncTaskExecutor = new TaskExecutor() {
            @Override
            public <V> Future<V> execute(BackgroundTask<V> task) {
                return Executors.newSingleThreadExecutor().submit(() -> {
                    V result = null;
                    try {
                        result = task.call();
                        task.getOnSuccess().accept(result);
                    } catch (Exception e) {
                        task.getOnException().accept(e);
                    }
                    return result;
                });
            }

            @Override
            public <V> Future<?> schedule(BackgroundTask<V> task, long delay, TimeUnit unit) {
                return Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                    execute(task);
                    return null;
                }, delay, unit);
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
        Pipeline<TestIdentifiable, TestIdentifiable> asyncPipeline = new Pipeline<>(asyncTaskExecutor);
        
        // Use a slow stage so we can verify the processing state
        TestPipelineStage stage = new TestPipelineStage("stage1") {
            @Override
            public TestIdentifiable process(TestIdentifiable obj) {
                try {
                    Thread.sleep(50); // Make processing slow enough to test
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return super.process(obj);
            }
        };
        asyncPipeline.addStage(stage);

        TestIdentifiable obj = new TestIdentifiable("test", 1);
        
        // First call - process the object
        PipelineObjectTracker<TestIdentifiable, TestIdentifiable> tracker1 = asyncPipeline.process(obj);
        
        // Wait for processing to complete and map cleanup
        Thread.sleep(100);
        assertTrue(tracker1.isFinished(), "First tracker should be finished");
        
        // Second call with same object after finished - should create new tracker
        // because the old one was removed from the map
        PipelineObjectTracker<TestIdentifiable, TestIdentifiable> tracker2 = asyncPipeline.process(obj);
        assertNotSame(tracker1, tracker2, "Same object after finished should create new tracker");
        // State should be PROCESSING immediately (processing hasn't started yet with async executor)
        assertTrue(tracker2.isProcessing(), "New tracker should be processing");
        
        // Wait for new processing to complete
        Thread.sleep(100);
        assertTrue(tracker2.isFinished(), "Second tracker should be finished");
    }

    @Test
    void testIdempotencyAfterError() throws InterruptedException {
        // Use async executor for this test to avoid timing issues
        TaskExecutor asyncTaskExecutor = new TaskExecutor() {
            @Override
            public <V> Future<V> execute(BackgroundTask<V> task) {
                return Executors.newSingleThreadExecutor().submit(() -> {
                    V result = null;
                    try {
                        result = task.call();
                        task.getOnSuccess().accept(result);
                    } catch (Exception e) {
                        task.getOnException().accept(e);
                    }
                    return result;
                });
            }

            @Override
            public <V> Future<?> schedule(BackgroundTask<V> task, long delay, TimeUnit unit) {
                return Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                    execute(task);
                    return null;
                }, delay, unit);
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
        Pipeline<TestIdentifiable, TestIdentifiable> asyncPipeline = new Pipeline<>(asyncTaskExecutor);
        
        // Stage that throws an exception
        TestPipelineStage errorStage = new TestPipelineStage("error") {
            @Override
            public TestIdentifiable process(TestIdentifiable obj) {
                throw new RuntimeException("Test error");
            }
        };
        asyncPipeline.addStage(errorStage);

        TestIdentifiable obj = new TestIdentifiable("test", 1);
        
        // First call - process the object (will error)
        PipelineObjectTracker<TestIdentifiable, TestIdentifiable> tracker1 = asyncPipeline.process(obj);
        
        // Wait for processing to complete with error and map cleanup
        Thread.sleep(50);
        assertTrue(tracker1.isError(), "First tracker should be in error state");
        
        // Second call with same object after error - should create new tracker
        // because the old one was removed from the map
        PipelineObjectTracker<TestIdentifiable, TestIdentifiable> tracker2 = asyncPipeline.process(obj);
        assertNotSame(tracker1, tracker2, "Same object after error should create new tracker");
        // State should be PROCESSING immediately
        assertTrue(tracker2.isProcessing(), "New tracker should be processing");
        
        // Wait for new processing (will error again)
        Thread.sleep(50);
        assertTrue(tracker2.isError(), "Second tracker should be in error state");
    }

    @Test
    void testIdempotencyDifferentObjects() throws InterruptedException {
        TestPipelineStage stage = new TestPipelineStage("stage1");
        pipeline.addStage(stage);

        // Two different objects with different IDs
        TestIdentifiable obj1 = new TestIdentifiable("obj1", 1);
        TestIdentifiable obj2 = new TestIdentifiable("obj2", 2);
        
        PipelineObjectTracker<TestIdentifiable, TestIdentifiable> tracker1 = pipeline.process(obj1);
        PipelineObjectTracker<TestIdentifiable, TestIdentifiable> tracker2 = pipeline.process(obj2);
        
        // Should get different trackers for different objects
        assertNotSame(tracker1, tracker2, "Different objects should get different trackers");
        
        // Wait for processing
        Thread.sleep(10);
        
        assertTrue(tracker1.isFinished());
        assertTrue(tracker2.isFinished());
    }

    /**
     * Test class implementing Identifiable for testing purposes.
     */
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

    /**
     * Test implementation of PipelineStage for testing purposes.
     */
    static class TestPipelineStage extends PipelineStage<TestIdentifiable, TestIdentifiable> {
        private final String stageName;

        TestPipelineStage(String stageName) {
            super("Processing " + stageName);
            this.stageName = stageName;
        }

        @Override
        public TestIdentifiable process(TestIdentifiable obj) {
            // Increment value and change name to stage name
            return new TestIdentifiable(stageName, obj.getValue() + 1);
        }
    }

    /**
     * Different identifiable type for type checking tests.
     */
    static class DifferentIdentifiable implements Identifiable {
        private final String id;

        DifferentIdentifiable(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }
    }

    /**
     * Pipeline stage that processes DifferentIdentifiable for type checking tests.
     */
    static class DifferentTypeStage extends PipelineStage<DifferentIdentifiable, DifferentIdentifiable> {
        private final String stageName;

        DifferentTypeStage(String stageName) {
            super("Processing " + stageName);
            this.stageName = stageName;
        }

        @Override
        public DifferentIdentifiable process(DifferentIdentifiable obj) {
            return new DifferentIdentifiable(stageName + "_" + obj.getId());
        }
    }
}
