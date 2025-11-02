package org.jabref.logic.pipeline;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Utility class for extracting type information from PipelineStage instances.
 * <p>
 * Uses reflection to determine input and output types for runtime type checking
 * and validation in pipeline construction.
 */
public class PipelineStageTypeUtil {
    /**
     * Gets the output type (U) from a PipelineStage.
     *
     * @param stage the pipeline stage
     * @return the output type class
     * @throws IllegalStateException if the output type cannot be determined
     */
    public static Class<?> getOutputType(PipelineStage stage) {
        // First, try to get from generic superclass (PipelineStage<T, U>)
        Class<?> type = extractTypeFromSuperclass(stage, 1);
        if (type != null) {
            return type;
        }

        // Fallback: try to get from the process method's return type
        return extractTypeFromProcessMethod(stage, true);
    }

    /**
     * Gets the input type (T) from a PipelineStage.
     *
     * @param stage the pipeline stage
     * @return the input type class
     * @throws IllegalStateException if the input type cannot be determined
     */
    public static Class<?> getInputType(PipelineStage stage) {
        // First, try to get from generic superclass (PipelineStage<T, U>)
        Class<?> type = extractTypeFromSuperclass(stage, 0);
        if (type != null) {
            return type;
        }

        // Fallback: try to get from the process method's parameter type
        return extractTypeFromProcessMethod(stage, false);
    }

    private static Class<?> extractTypeFromSuperclass(PipelineStage stage, int typeArgIndex) {
        Class<?> currentClass = stage.getClass();
        while (currentClass != null && currentClass != Object.class) {
            Type genericSuperclass = currentClass.getGenericSuperclass();
            if (genericSuperclass instanceof ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType) genericSuperclass;
                if (paramType.getRawType().equals(PipelineStage.class)) {
                    Type[] typeArgs = paramType.getActualTypeArguments();
                    if (typeArgs.length > typeArgIndex && typeArgs[typeArgIndex] instanceof Class) {
                        return (Class<?>) typeArgs[typeArgIndex];
                    }
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        return null;
    }

    private static Class<?> extractTypeFromProcessMethod(PipelineStage stage, boolean returnType) {
        Method[] methods = stage.getClass().getMethods();
        for (Method method : methods) {
            if (method.getName().equals("process") && method.getParameterCount() == 1) {
                if (returnType) {
                    // Try to get generic return type first
                    Type genericReturnType = method.getGenericReturnType();
                    if (genericReturnType instanceof Class) {
                        return (Class<?>) genericReturnType;
                    } else if (genericReturnType instanceof ParameterizedType) {
                        // For ParameterizedType, get the raw type
                        return (Class<?>) ((ParameterizedType) genericReturnType).getRawType();
                    }
                    return method.getReturnType();
                } else {
                    // Try to get generic parameter type first
                    Type[] genericParameterTypes = method.getGenericParameterTypes();
                    if (genericParameterTypes.length > 0) {
                        Type paramType = genericParameterTypes[0];
                        if (paramType instanceof Class) {
                            return (Class<?>) paramType;
                        } else if (paramType instanceof ParameterizedType) {
                            // For ParameterizedType, get the raw type
                            return (Class<?>) ((ParameterizedType) paramType).getRawType();
                        }
                    }
                    return method.getParameterTypes()[0];
                }
            }
        }
        String typeName = returnType ? "output" : "input";
        throw new IllegalStateException("Could not determine " + typeName + " type for stage: " + stage.getClass().getName());
    }
}
