package com.sinux.happy.orm.dal;

/**
 * 用于在Where条件中表示包含关系的运算符
 *
 * @author zhaosh
 * @since 2018-07-20
 */
public enum Relation {
    In("IN"),
    NotIn("NOT IN");

    private String s;

    Relation(String str) {
        this.s = str;
    }

    @Override
    public String toString() {
        return this.s;
    }
}
