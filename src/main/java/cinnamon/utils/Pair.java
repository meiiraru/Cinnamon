package cinnamon.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record Pair<F, S>(F first, S second) {

    public static <F, S> Pair<F, S> of(F first, S second) {
        return new Pair<>(first, second);
    }

    public Pair<S, F> swap() {
        return of(second, first);
    }

    public static <F, S> List<Pair<F, S>> zip(Map<F, S> map) {
        final List<Pair<F, S>> pairs = new ArrayList<>(map.size());
        for (var entry : map.entrySet())
            pairs.add(of(entry.getKey(), entry.getValue()));
        return pairs;
    }

    public static <F, S> Map<F, S> unzip(List<Pair<F, S>> pairs) {
        final Map<F, S> map = new HashMap<>(pairs.size());
        for (Pair<F, S> pair : pairs)
            map.put(pair.first(), pair.second());
        return map;
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
