package com.sinux.happy.orm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用来标记某个字段与数据库之间的映射关系
 *
 * @author lihp
 * @date 2018/7/10
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
    /**
     * 数据库中的字段名，如果字段名和当前get方法的名称一样，可以省略
     * 例如getId()与数据库中的id字段对应时，就可以简写为@Column
     *
     * @return
     */
    String value() default "";

    /**
     * 如果引用了其他表，可以指定与数据库中哪个字段关联
     * 如果引用的字段有ForeignKey注解，可以省略
     * 另外，也可以是目标类型中不存在，但是在表中实际存在的字段
     * 例如
     *
     * @return
     * @Table("tb_foo") class Foo {
     * private Bar bar;
     * <p>
     * // 当Bar类中有且仅有一个ForeignKey注解时，
     * // 可以省略Foo类中getBar()的Column注解的refField属性
     * @Column("bar_id") public Bar getBar(){
     * return bar;
     * }
     * <p>
     * setter省略...
     * <p>
     * // 指定这个Bar是通过tb_bar.name字段关联的
     * // 这种情况不能省略
     * @Column(value="bar_name", refField="name")
     * public Bar getBar2(){
     * <p>
     * }
     * <p>
     * setter省略...
     * }
     * @Table("tb_bar") class Bar {
     * private String id;
     * private String name;
     * @Column
     * @ForeignKey public String getId(){
     * return id;
     * }
     * <p>
     * setter省略...
     * @Column public String gerName(){
     * return name;
     * }
     * <p>
     * setter省略...
     * }
     */
    String refField() default "";

    /**
     * 如果引用了其他表，可以指定与返回的Entity的哪个getMethod关联
     * 规则同refField，与refField不可同时使用。只不过是对应方法名。
     *
     * @return
     */
    String refColumnMethod() default "";
}
