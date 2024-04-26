package mayo.utils;

import java.util.Objects;

public record Pair<F, S>(F first, S second) {

    public static <F, S> Pair<F, S> of(final F first, final S second) {
        return new Pair<>(first, second);
    }

    public Pair<S, F> swap() {
        return of(second, first);
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof Pair<?, ?> other && Objects.equals(first, other.first) && Objects.equals(second, other.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    @Override
    public String toString() {
        return "(" + first + ", " + second + ")";
    }
}
