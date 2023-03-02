package utils;

import java.io.Serializable;

public class Pair<K,V> implements Serializable {
    private K key;
    private V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public V getValue() {
        return value;
    }

    public K getKey() {
        return key;
    }

    public String toString() {
        return "Pair<" + key + ", "+value+">";
    }
}