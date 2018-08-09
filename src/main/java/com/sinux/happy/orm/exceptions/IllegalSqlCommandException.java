package com.sinux.happy.orm.exceptions;

public class IllegalSqlCommandException extends IllegalAccessException {
    public IllegalSqlCommandException(String msg) {
        super(msg);
    }
}
