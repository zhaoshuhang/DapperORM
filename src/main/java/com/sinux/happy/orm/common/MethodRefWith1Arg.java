package com.sinux.happy.orm.common;

@FunctionalInterface
public interface MethodRefWith1Arg<T> {
    void call(T t);
}
