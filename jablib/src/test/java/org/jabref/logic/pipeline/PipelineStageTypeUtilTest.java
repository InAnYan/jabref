package org.jabref.logic.pipeline;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PipelineStageTypeUtilTest {

    @Test
    void testGetInputTypeFromConcreteStage() {
        TestPipelineStage stage = new TestPipelineStage("test");
        Class<?> inputType = PipelineStageTypeUtil.getInputType(stage);

        assertEquals(TestIdentifiable.class, inputType);
    }

    @Test
    void testGetOutputTypeFromConcreteStage() {
        TestPipelineStage stage = new TestPipelineStage("test");
        Class<?> outputType = PipelineStageTypeUtil.getOutputType(stage);

        assertEquals(TestIdentifiable.class, outputType);
    }

    @Test
    void testGetInputTypeFromDifferentTypeStage() {
        DifferentTypeStage stage = new DifferentTypeStage("test");
        Class<?> inputType = PipelineStageTypeUtil.getInputType(stage);

        assertEquals(DifferentIdentifiable.class, inputType);
    }

    @Test
    void testGetOutputTypeFromDifferentTypeStage() {
        DifferentTypeStage stage = new DifferentTypeStage("test");
        Class<?> outputType = PipelineStageTypeUtil.getOutputType(stage);

        assertEquals(DifferentIdentifiable.class, outputType);
    }

    @Test
    void testGetInputTypeFromAnonymousStage() {
        TestPipelineStage stage = new TestPipelineStage("test") {
            @Override
            public TestIdentifiable process(TestIdentifiable obj) {
                return new TestIdentifiable("processed", 42);
            }
        };
        Class<?> inputType = PipelineStageTypeUtil.getInputType(stage);

        assertEquals(TestIdentifiable.class, inputType);
    }

    @Test
    void testGetOutputTypeFromAnonymousStage() {
        TestPipelineStage stage = new TestPipelineStage("test") {
            @Override
            public TestIdentifiable process(TestIdentifiable obj) {
                return new TestIdentifiable("processed", 42);
            }
        };
        Class<?> outputType = PipelineStageTypeUtil.getOutputType(stage);

        assertEquals(TestIdentifiable.class, outputType);
    }

    @Test
    void testGetInputTypeFromStageWithSuperType() {
        // Stage that accepts a supertype (Identifiable) but processes TestIdentifiable
        PipelineStage<Identifiable, TestIdentifiable> stage = new PipelineStage<Identifiable, TestIdentifiable>() {
            @Override
            public TestIdentifiable process(Identifiable obj) {
                return new TestIdentifiable("processed", 42);
            }
        };
        Class<?> inputType = PipelineStageTypeUtil.getInputType(stage);

        assertEquals(Identifiable.class, inputType);
    }

    @Test
    void testGetOutputTypeFromStageWithSuperType() {
        // Stage that accepts Identifiable but returns TestIdentifiable
        PipelineStage<Identifiable, TestIdentifiable> stage = new PipelineStage<Identifiable, TestIdentifiable>() {
            @Override
            public TestIdentifiable process(Identifiable obj) {
                return new TestIdentifiable("processed", 42);
            }
        };
        Class<?> outputType = PipelineStageTypeUtil.getOutputType(stage);

        assertEquals(TestIdentifiable.class, outputType);
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
