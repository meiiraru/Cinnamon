package cinnamon.utils;

/**
 * Interface representing a function that accepts three arguments and produces no result<br>
 * This is the three-arity specialization of {@link java.util.function.Consumer}
 * @param <T> The type of the first argument to the operation
 * @param <U> The type of the second argument to the operation
 * @param <V> The type of the third argument to the operation
 */
public interface TriConsumer<T, U, V> {
    void accept(T t, U u, V v);
}
