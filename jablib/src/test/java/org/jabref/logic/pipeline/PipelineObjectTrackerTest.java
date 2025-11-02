package org.jabref.logic.pipeline;

import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipelineObjectTrackerTest {

    @Test
    void testInitialState() {
        TestIdentifiable input = new TestIdentifiable("input", 0);
        PipelineObjectTracker<TestIdentifiable, TestIdentifiable> tracker = new PipelineObjectTracker<>(input);
        assertEquals(PipelineObjectTracker.State.PROCESSING, tracker.getState());
        assertFalse(tracker.isComplete());
        assertTrue(tracker.isProcessing());
        assertFalse(tracker.isFinished());
        assertFalse(tracker.isError());
        assertEquals(Optional.empty(), tracker.getResult());
        assertEquals(Optional.empty(), tracker.getError());
        assertEquals(input, tracker.getInputObject());
    }

    @Test
    void testFinishedState() {
        TestIdentifiable input = new TestIdentifiable("input", 0);
        PipelineObjectTracker<TestIdentifiable, TestIdentifiable> tracker = new PipelineObjectTracker<>(input);
        TestIdentifiable result = new TestIdentifiable("result", 1);
        tracker.resultProperty().set(result);
        tracker.stateProperty().set(PipelineObjectTracker.State.FINISHED);

        assertEquals(PipelineObjectTracker.State.FINISHED, tracker.getState());
        assertTrue(tracker.isComplete());
        assertFalse(tracker.isProcessing());
        assertTrue(tracker.isFinished());
        assertFalse(tracker.isError());
        assertEquals(Optional.of(result), tracker.getResult());
        assertEquals(Optional.empty(), tracker.getError());
        assertEquals(input, tracker.getInputObject());
    }

    @Test
    void testErrorState() {
        TestIdentifiable input = new TestIdentifiable("input", 0);
        PipelineObjectTracker<TestIdentifiable, TestIdentifiable> tracker = new PipelineObjectTracker<>(input);
        Exception error = new RuntimeException("Test error");
        tracker.errorProperty().set(error);
        tracker.stateProperty().set(PipelineObjectTracker.State.ERROR);

        assertEquals(PipelineObjectTracker.State.ERROR, tracker.getState());
        assertTrue(tracker.isComplete());
        assertFalse(tracker.isProcessing());
        assertFalse(tracker.isFinished());
        assertTrue(tracker.isError());
        assertEquals(Optional.empty(), tracker.getResult());
        assertEquals(Optional.of(error), tracker.getError());
        assertEquals(input, tracker.getInputObject());
    }

    @Test
    void testStateProperty() {
        TestIdentifiable input = new TestIdentifiable("input", 0);
        PipelineObjectTracker<TestIdentifiable, TestIdentifiable> tracker = new PipelineObjectTracker<>(input);
        ObjectProperty<PipelineObjectTracker.State> stateProperty = tracker.stateProperty();
        assertEquals(PipelineObjectTracker.State.PROCESSING, stateProperty.get());

        stateProperty.set(PipelineObjectTracker.State.FINISHED);
        assertEquals(PipelineObjectTracker.State.FINISHED, tracker.getState());
    }

    @Test
    void testResultProperty() {
        TestIdentifiable input = new TestIdentifiable("input", 0);
        PipelineObjectTracker<TestIdentifiable, TestIdentifiable> tracker = new PipelineObjectTracker<>(input);
        ObjectProperty<TestIdentifiable> resultProperty = tracker.resultProperty();
        assertEquals(Optional.empty(), tracker.getResult());

        TestIdentifiable result = new TestIdentifiable("result", 1);
        resultProperty.set(result);
        assertEquals(Optional.of(result), tracker.getResult());
    }

    @Test
    void testErrorProperty() {
        TestIdentifiable input = new TestIdentifiable("input", 0);
        PipelineObjectTracker<TestIdentifiable, TestIdentifiable> tracker = new PipelineObjectTracker<>(input);
        ObjectProperty<Exception> errorProperty = tracker.errorProperty();
        assertEquals(Optional.empty(), tracker.getError());

        Exception error = new RuntimeException("Test error");
        errorProperty.set(error);
        assertEquals(Optional.of(error), tracker.getError());
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
}
