package com.sinux.happy.orm.exceptions;

/**
 * @author zhaosh
 * @since 2018-07-20
 */
public class TooManyResultsException extends RuntimeException {
    public TooManyResultsException(String message) {
        super(message);
    }
}
