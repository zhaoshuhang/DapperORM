package com.sinux.happy.orm.exceptions;

/**
 * @author zhaosh
 * @since 2018-07-20
 */
public class TableNotFoundException extends RuntimeException {
    public TableNotFoundException(String className) {
        super(className + "沒有指定表明");
    }
}
