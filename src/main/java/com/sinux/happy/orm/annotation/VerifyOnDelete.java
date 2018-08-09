package com.sinux.happy.orm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于标记当前字段会在执行delete的时候作为where条件
 * 常见的一个场景是将注解应用在getId方法上。
 * 在delete的时候会自动生成 where id = #{id}
 *
 * @author lihp
 * @date 2018/7/10
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface VerifyOnDelete {
}
