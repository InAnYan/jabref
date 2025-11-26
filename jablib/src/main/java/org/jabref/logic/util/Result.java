package org.jabref.logic.util;

/// A sealed interface representing a result of a computation that can either
/// succeed with a value of type T or fail with an error of type E.
///
/// Similar to Either monad or [Result](https://doc.rust-lang.org/std/result/) in Rust
///
/// @param <T> Type of the success value
/// @param <E> Type of the error
public sealed interface Result<T, E> permits Result.Ok, Result.Err {

    /// Checks if this result is a success.
    ///
    /// @return true if this is an Ok result, false otherwise
    boolean isOk();

    /// Checks if this result is an error.
    ///
    /// @return true if this is an Err result, false otherwise
    boolean isErr();

    /// Returns the success value.
    ///
    /// @return the value of type T
    /// @throws IllegalStateException if this result is an error
    T getValue();

    /// Returns the error value.
    ///
    /// @return the error of type E
    /// @throws IllegalStateException if this result is a success
    E getError();

    /// Creates a success result containing the given value.
    ///
    /// @param value the success value
    /// @param <T>   Type of the success value
    /// @param <E>   Type of the error
    /// @return an Ok result
    static <T, E> Result<T, E> ok(T value) {
        return new Ok<>(value);
    }

    /// Creates an error result containing the given error.
    ///
    /// @param error the error value
    /// @param <T>   Type of the success value
    /// @param <E>   Type of the error
    /// @return an Err result
    static <T, E> Result<T, E> err(E error) {
        return new Err<>(error);
    }

    /// Represents a success result containing a value of type T.
    ///
    /// @param <T> Type of the success value
    /// @param <E> Type of the error
    final class Ok<T, E> implements Result<T, E> {
        private final T value;

        /// Constructs a success result with the given value.
        ///
        /// @param value the success value
        public Ok(T value) {
            this.value = value;
        }

        @Override
        public boolean isOk() {
            return true;
        }

        @Override
        public boolean isErr() {
            return false;
        }

        @Override
        public T getValue() {
            return value;
        }

        @Override
        public E getError() {
            throw new IllegalStateException("No error in Ok");
        }

        @Override
        public String toString() {
            return "Ok(" + value + ")";
        }
    }

    /// Represents an error result containing a value of type E.
    ///
    /// @param <T> Type of the success value
    /// @param <E> Type of the error
    final class Err<T, E> implements Result<T, E> {
        private final E error;

        /// Constructs an error result with the given error value.
        ///
        /// @param error the error value
        public Err(E error) {
            this.error = error;
        }

        @Override
        public boolean isOk() {
            return false;
        }

        @Override
        public boolean isErr() {
            return true;
        }

        @Override
        public T getValue() {
            throw new IllegalStateException("No value in Err");
        }

        @Override
        public E getError() {
            return error;
        }

        @Override
        public String toString() {
            return "Err(" + error + ")";
        }
    }
}
