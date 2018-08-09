package com.sinux.happy.orm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * 用于标记当前字段会在执行update的时候作为where条件
 * 常见的一个场景是将注解应用在时间戳方法上,来避免同时修改某个数据
 * 在delete的时候会自动生成类似于 where dataChangeLastTime = #{dataChangeLastTime}
 *
 * @author lihp
 * @date 2018/7/10
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface VerifyOnUpdate {
}
