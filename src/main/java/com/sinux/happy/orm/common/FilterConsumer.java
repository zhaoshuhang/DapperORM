package com.sinux.happy.orm.common;

@FunctionalInterface
public interface FilterConsumer<T> {
    boolean accept(T o);
}