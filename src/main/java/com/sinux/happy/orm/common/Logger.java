package com.sinux.happy.orm.common;

import java.util.logging.Level;

public class Logger {
    public static java.util.logging.Logger inst = java.util.logging.Logger.getLogger("HappyORM");

    public static void logError(String msg) {
        inst.log(Level.SEVERE, msg);
    }

    public static void logError(Throwable e) {
        inst.log(Level.SEVERE, e.getMessage(), e);
    }

    public static void logError(String msg, Throwable e) {
        inst.log(Level.SEVERE, msg, e);
    }

    public static void info(String msg){
        inst.info(msg);
    }
}
