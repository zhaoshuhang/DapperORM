package com.sinux.happy.orm.common;

import java.util.function.Consumer;

/**
 * 一个允许使用Throw的method作为lambda表达式成员的Consumer
 *
 * @author zhaosh
 * @since 2018-07-20
 */
@FunctionalInterface
public interface ThrowingConsumer<T> extends Consumer<T> {

    @Override
    default void accept(final T elem) {
        try {
            acceptThrows(elem);
        } catch (final Exception e) {
            Logger.logError(e);
            throw new RuntimeException(e);
        }
    }

    void acceptThrows(T elem) throws Exception;
}
