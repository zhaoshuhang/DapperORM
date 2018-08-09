package com.sinux.happy.orm.common;

import com.sinux.happy.orm.dal.WhereCondition;

/**
 * 用于构造Where条件的lambda表达式的Consumer
 *
 * @author zhaosh
 * @since 2018-07-20
 */
@FunctionalInterface
public interface WhereLoopConsumer<T, N> {
    WhereCondition<T> accept(N obj, WhereCondition<T> o);
}
