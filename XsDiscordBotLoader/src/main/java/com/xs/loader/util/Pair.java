package com.xs.loader.util;

public class Pair<K, V> {
    private K KEY;
    private V VALUE;

    public Pair(final K KEY, final V VALUE) {
        this.KEY = KEY;
        this.VALUE = VALUE;
    }

    public K getKey() {
        return KEY;
    }

    public V getValue() {
        return VALUE;
    }

    public void clear() {
        KEY = null;
        VALUE = null;
    }
}
