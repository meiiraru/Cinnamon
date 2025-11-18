package cinnamon.world.ai;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class AStar {

    protected AStar() {}

    public static <T> List<T> findPath(T start, T goal, Function<T, Iterable<T>> neighbors, BiFunction<T, T, Float> heuristic) {
        return findPath(start, goal, neighbors, heuristic, (a, b) -> 1.0f);
    }

    public static <T> List<T> findPath(T start, T goal, Function<T, Iterable<T>> neighbors, BiFunction<T, T, Float> heuristic, BiFunction<T, T, Float> stepCost) {
        if (Objects.equals(start, goal))
            return List.of(start);

        Map<T, Float> gScore = new HashMap<>();
        Map<T, Float> fScore = new HashMap<>();
        Map<T, T> cameFrom   = new HashMap<>();

        PriorityQueue<T> toVisit = new PriorityQueue<>(Comparator.comparingDouble(n -> fScore.getOrDefault(n, Float.POSITIVE_INFINITY)));

        gScore.put(start, 0f);
        fScore.put(start, heuristic.apply(start, goal));
        toVisit.add(start);

        //visited marker
        Set<T> visited = new HashSet<>();

        while (!toVisit.isEmpty()) {
            T current = toVisit.poll();
            if (Objects.equals(current, goal))
                return bakePath(cameFrom, current);

            //set as visited and skip if it was already visited
            if (!visited.add(current))
                continue;

            for (T nb : neighbors.apply(current)) {
                if (visited.contains(nb))
                    continue;

                float tentativeG = gScore.getOrDefault(current, Float.POSITIVE_INFINITY) + stepCost.apply(current, nb);

                if (tentativeG < gScore.getOrDefault(nb, Float.POSITIVE_INFINITY)) {
                    cameFrom.put(nb, current);
                    gScore.put(nb, tentativeG);
                    fScore.put(nb, tentativeG + heuristic.apply(nb, goal));

                    //update priority
                    toVisit.remove(nb);
                    toVisit.add(nb);
                }
            }
        }

        return Collections.emptyList();
    }

    protected static <T> List<T> bakePath(Map<T, T> cameFrom, T current) {
        ArrayDeque<T> stack = new ArrayDeque<>();
        stack.push(current);

        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            stack.push(current);
        }

        List<T> path = new ArrayList<>(stack.size());
        while (!stack.isEmpty())
            path.add(stack.pop());

        return path;
    }
}
