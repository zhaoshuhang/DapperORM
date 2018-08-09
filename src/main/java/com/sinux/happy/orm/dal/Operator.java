package com.sinux.happy.orm.dal;

/**
 * 用于在Where条件中表示关系的运算符
 *
 * @author zhaosh
 * @since 2018-07-20
 */
public enum Operator {
    Equals("="),
    LargerThan(">"),
    LessThan("<"),
    LargerOrEqu(">="),
    LessOrEqu("<="),
    NotEquals("<>"),
    Like(" LIKE "),
    NotLike(" NOT LIKE ");

    private String s;

    Operator(String str) {
        this.s = str;
    }

    @Override
    public String toString() {
        return this.s;
    }
}
