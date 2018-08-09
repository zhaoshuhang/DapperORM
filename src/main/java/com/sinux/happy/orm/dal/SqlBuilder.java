package com.sinux.happy.orm.dal;

/**
 * 构造SQL语句的统一入口
 *
 * @author zhaosh
 * @since 2018-07-20
 */
public class SqlBuilder {
    public static <T> Select<T> select(Class<T> clazz) {
        return new Select<>(clazz);
    }

    public static <T> Sql<T> insert(T data) {
        Insert<T> insert = new Insert<>(data);
        return insert.toSql();
    }

    public static <T> Update<T> update(T data) {
        return new Update<>(data);
    }

    public static <T> Delete<T> delete(T data) {
        return new Delete<>(data);
    }

    public static <T> Delete<T> delete(Class<T> clazz) {
        return new Delete<>(clazz);
    }
}
