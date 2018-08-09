package com.sinux.happy.orm.common;

public class Pair<TKey, TValue> {
    private TKey key;
    private TValue value;

    public Pair(TKey key, TValue value) {
        this.key = key;
        this.value = value;
    }

    public TKey getKey() {
        return key;
    }

    public void setKey(TKey key) {
        this.key = key;
    }

    public TValue getValue() {
        return value;
    }

    public void setValue(TValue value) {
        this.value = value;
    }
}
