package main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;

public class SpadPrelude {
    private SpadPrelude() {}

    public static Object print(Object value) {
        System.out.println(value);
        return value;
    }

    public static void directive(String name, Object... args) {
        System.out.println("[directive] " + name + " " + java.util.Arrays.toString(args));
    }

    public static int toInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    public static double toFloat(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }

    public static String toStringValue(Object value) {
        return String.valueOf(value);
    }

    public static boolean truthy(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue() != 0.0;
        }
        if (value instanceof CharSequence) {
            return ((CharSequence) value).length() > 0;
        }
        if (value instanceof java.util.Collection<?>) {
            return !((java.util.Collection<?>) value).isEmpty();
        }
        if (value instanceof java.util.Map<?, ?>) {
            return !((java.util.Map<?, ?>) value).isEmpty();
        }
        return true;
    }

    public static boolean eq(Object left, Object right) {
        return Objects.equals(left, right);
    }

    public static Map<String, Integer> dijkstra(
            Map<String, Map<String, Integer>> graph,
            String start
    ) {
        Map<String, Integer> distances = new HashMap<>();
        for (String node : graph.keySet()) {
            distances.put(node, Integer.MAX_VALUE);
        }
        distances.put(start, 0);

        PriorityQueue<NodeDistance> pq = new PriorityQueue<>((a, b) -> Integer.compare(a.distance, b.distance));
        pq.add(new NodeDistance(start, 0));

        while (!pq.isEmpty()) {
            NodeDistance current = pq.poll();
            if (current.distance > distances.getOrDefault(current.node, Integer.MAX_VALUE)) {
                continue;
            }

            Map<String, Integer> neighbors = graph.getOrDefault(current.node, Collections.emptyMap());
            for (Map.Entry<String, Integer> edge : neighbors.entrySet()) {
                int candidate = current.distance + edge.getValue();
                if (candidate < distances.getOrDefault(edge.getKey(), Integer.MAX_VALUE)) {
                    distances.put(edge.getKey(), candidate);
                    pq.add(new NodeDistance(edge.getKey(), candidate));
                }
            }
        }

        return distances;
    }

    public static List<Object> list(Object... values) {
        List<Object> out = new ArrayList<>();
        Collections.addAll(out, values);
        return out;
    }

    private static class NodeDistance {
        final String node;
        final int distance;

        NodeDistance(String node, int distance) {
            this.node = node;
            this.distance = distance;
        }
    }
}
