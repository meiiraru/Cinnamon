package cinnamon.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Trie<T> {

    private final Node<T> root;

    public Trie() {
        this.root = new Node<>();
    }

    public void insert(String word, T value) {
        Node<T> current = root;
        for (char ch : word.toCharArray())
            current = current.children.computeIfAbsent(ch, c -> new Node<>());
        current.value = value;
    }

    public T get(String word) {
        Node<T> current = root;
        for (char ch : word.toCharArray()) {
            if (!current.children.containsKey(ch))
                return null;
            current = current.children.get(ch);
        }
        return current.value;
    }

    public boolean contains(String word) {
        return get(word) != null;
    }

    public List<String> getWords(String prefix) {
        Node<T> current = root;
        for (char ch : prefix.toCharArray()) {
            if (!current.children.containsKey(ch))
                return List.of();
            current = current.children.get(ch);
        }
        return getWords(current, prefix);
    }

    private List<String> getWords(Node<T> node, String prefix) {
        List<String> words = new ArrayList<>();
        if (node.value != null)
            words.add(prefix);

        for (Map.Entry<Character, Node<T>> entry : node.children.entrySet())
            words.addAll(getWords(entry.getValue(), prefix + entry.getKey()));

        return words;
    }

    private static class Node<T> {
        private final Map<Character, Node<T>> children = new HashMap<>();
        private T value;
    }
}
