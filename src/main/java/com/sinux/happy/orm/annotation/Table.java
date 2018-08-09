package com.sinux.happy.orm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于标记一个Class是映射到数据库的某个表
 *
 * @author lihp
 * @date 2018/4/19
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Table {
    /**
     * 表名
     *
     * @return
     */
    String value() default "";
}
