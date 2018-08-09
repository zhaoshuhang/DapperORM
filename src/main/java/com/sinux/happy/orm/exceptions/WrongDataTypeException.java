package com.sinux.happy.orm.exceptions;

/**
 * @author zhaosh
 * @since 2018-07-20
 */
public class WrongDataTypeException extends RuntimeException {
    public WrongDataTypeException(String msg) {
        super(msg);
    }
}
