package com.sinux.happy.orm.dal;

import com.sinux.happy.orm.annotation.Column;
import com.sinux.happy.orm.annotation.Table;
import com.sinux.happy.orm.common.*;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zhaosh
 * @since 2018-07-20
 */
public abstract class WhereCondition<T> {
    private final Class<T> cls;
    private StringBuilder whereSqlBuilder;
    private String orderBy = null;
    private Long limit = null;
    private Long skip = null; 
    private String tableName = null;
    private String count = null;
    
    protected WhereCondition(Class<T> clazz) {
        if (null == clazz) {
            throw new NullArgumentException("clazz");
        }
        this.cls = clazz;
        this.whereSqlBuilder = new StringBuilder();
        Table table = clazz.getAnnotation(Table.class);
        if (null == table || null == table.value() || table.value().length() == 0) {
            throw new IllegalArgumentException("Class必须有有效的Table注解！" + clazz.getName() + "缺少有效的Table注解！");
        }

        this.tableName = table.value();
    }

    public abstract Sql<T> toSql();

    protected String getWhereCondition() {
        return this.whereSqlBuilder.toString();
    }

    protected String getOrderBy() {
        return null == this.orderBy ? "" : this.orderBy;
    }

    

    protected Class<T> getCls() {
        return this.cls;
    }

    protected String getTableName() {
        return this.tableName;
    }

    public WhereCondition<T> and() {
        whereBuilderAppend("AND");

        return this;
    }

    public WhereCondition<T> or() {
        whereBuilderAppend("OR");

        return this;
    }

    protected String getCount() {
        return this.count;
    }

    protected String getLimit() {
        boolean isSkip = !(null == this.skip || this.skip <= 0);
        boolean isLimit = !(null == this.limit || this.limit <= 0);
        
        if(isSkip && isLimit){
            return "LIMIT " + this.skip + "," + this.limit;
        }

        if(isSkip){
            return "LIMIT " + this.skip+",-1";
        }

        if(isLimit){
            return "LIMIT " + this.limit;
        }

        return "";
    }

    public <N> WhereCondition<T> and(Iterable<N> parameters, WhereLoopConsumer<T, N> whereCondition) {
        if (null == whereCondition || null == parameters) {
            return this;
        }

        whereBuilderAppend("AND");
        this.whereSqlBuilder.append(" ( ");
        for (N p : parameters) {
            whereBuilderAppend("AND");
            whereCondition.accept(p, this);
        }
        this.whereSqlBuilder.append(" ) ");

        return this;
    }

    public WhereCondition<T> and(WhereConsumer<T>... whereConditions) {
        if (null == whereConditions || whereConditions.length == 0) {
            return this;
        }

        whereBuilderAppend("AND");

        this.whereSqlBuilder.append(" ( ");
        whereConditions[0].accept(this);
        for (int i = 1; i < whereConditions.length; i++) {
            whereBuilderAppend("AND");
            whereConditions[i].accept(this);
        }

        this.whereSqlBuilder.append(" ) ");

        return this;
    }

    public <N> WhereCondition<T> or(Iterable<N> parameters, WhereLoopConsumer<T, N> whereCondition) {
        if (null == whereCondition || null == parameters) {
            return this;
        }

        whereBuilderAppend("AND");
        this.whereSqlBuilder.append(" ( ");
        for (N p : parameters) {
            whereBuilderAppend("OR");
            whereCondition.accept(p, this);
        }
        this.whereSqlBuilder.append(" ) ");

        return this;
    }

    public WhereCondition<T> or(WhereConsumer<T>... whereConditions) {
        if (null == whereConditions || whereConditions.length == 0) {
            return this;
        }

        whereBuilderAppend("AND");

        this.whereSqlBuilder.append(" ( ");
        whereConditions[0].accept(this);
        for (int i = 1; i < whereConditions.length; i++) {
            whereBuilderAppend("OR");
            whereConditions[i].accept(this);
        }

        this.whereSqlBuilder.append(" ) ");

        return this;
    }

    private void whereBuilderAppend(String key) {
        if (this.whereSqlBuilder.length() > 0) {
            String sql = this.whereSqlBuilder.toString().trim().toUpperCase();
            if (sql.endsWith(" " + key) || sql.endsWith("(")) {
                return;
            }

            if (sql.endsWith(" AND") || sql.endsWith(" OR")) {
                return;
            }

            this.whereSqlBuilder.append(" " + key + " ");
        }
    }

    public WhereCondition<T> where(MethodRefWith1Arg<T> fieldGetterMethod, Operator operator, Object value) {
        String columnName = getColumnName(fieldGetterMethod);

        return where("@" + columnName, operator, value);
    }

    public WhereCondition<T> where(String fieldName, Operator operator, Object value) {
        String columnName = getColumnName(fieldName);
        if (StringUtils.isBlank(columnName)) {
            throw new IllegalArgumentException("在" + cls.getName() + "中找不到名为" + fieldName + "的字段");
        }

        if (null == value) {
            if (operator == Operator.Equals) {
                return this.where(String.format("`%s`.`%s` is null", this.tableName, columnName));
            } else if (operator == Operator.NotEquals) {
                return this.where(String.format("`%s`.`%s` is not null", this.tableName, columnName));
            }
        }

        if (operator == Operator.Like || operator == Operator.NotLike) {
            if (!(value instanceof CharSequence)) {
                throw new IllegalArgumentException("Like只能用于String");
            }
        }

        return this.where(String.format("`%s`.`%s` %s %s", this.tableName, columnName, operator,
                Utils.convertObjToSqlString(value)));
    }

    public <E> WhereCondition<T> where(MethodRefWith1Arg<T> fieldGetterMethod, Relation relation, E[] collection) {
        String columnName = getColumnName(fieldGetterMethod);

        return where("@" + columnName, relation, collection);
    }

    private String getColumnName(MethodRefWith1Arg<T> fieldGetterMethod) {
        Method m = MethodUtils.getReferencedMethod(this.cls, fieldGetterMethod);
        if (null == m) {
            throw new IllegalArgumentException("在" + cls.getName() + "中找不到getter方法");
        }

        String columnName = Utils.getColumnName(m);
        if (StringUtils.isBlank(columnName)) {
            throw new IllegalArgumentException("在" + cls.getName() + "中找不到" + m.getName() + "对应的数据库字段名");
        }

        return columnName;
    }

    public <E> WhereCondition<T> where(String fieldName, Relation relation, E[] collection) {
        List<E> list = new ArrayList<>();
        for (E e : collection) {
            list.add(e);
        }

        return where(fieldName, relation, list);
    }

    public <E> WhereCondition<T> where(MethodRefWith1Arg<T> fieldGetterMethod, Relation relation,
            Iterable<E> collection) {
        String columnName = getColumnName(fieldGetterMethod);

        return where("@" + columnName, relation, collection);
    }

    public <E> WhereCondition<T> where(String fieldName, Relation relation, Iterable<E> collection) {
        String columnName = getColumnName(fieldName);
        if (StringUtils.isBlank(columnName)) {
            throw new IllegalArgumentException("在" + cls.getName() + "中找不到名为" + fieldName + "的字段");
        }

        List<String> list = new ArrayList<>();
        for (E o : collection) {
            list.add(Utils.convertObjToSqlString(o));
        }

        return this.where(
                String.format("`%s`.`%s` %s (%s)", this.tableName, columnName, relation, String.join(",", list)));
    }

    private WhereCondition<T> where(String whereSql) {
        whereBuilderAppend("AND");
        this.whereSqlBuilder.append(" (");
        this.whereSqlBuilder.append(whereSql);
        this.whereSqlBuilder.append(") ");

        return this;
    }

    public WhereCondition<T> orderByASC(MethodRefWith1Arg<T> fieldGetterMethod,
            MethodRefWith1Arg<T>... otherFieldGetterMethods) {
        return orderBy("ASC", fieldGetterMethod, otherFieldGetterMethods);
    }

    public WhereCondition<T> orderByASC(String fieldName, String... otherFieldNames) {
        return orderBy("ASC", fieldName, otherFieldNames);
    }

    public WhereCondition<T> orderByDESC(MethodRefWith1Arg<T> fieldGetterMethod,
            MethodRefWith1Arg<T>... otherFieldGetterMethods) {
        return orderBy("DESC", fieldGetterMethod, otherFieldGetterMethods);
    }

    public WhereCondition<T> orderByDESC(String fieldName, String... otherFieldNames) {
        return orderBy("DESC", fieldName, otherFieldNames);
    }

    /**
     * 查询指定起始条数
     * @param count 起始条数
     * @return
     */
    public WhereCondition<T> skip(long count){
    	this.limit = count;
        return this;
    }
    
    /**
     * 根据skip函数指定起始条数查询显示条数
     * @param count 显示条数
     * @return
     */
    public WhereCondition<T> limit(long count){
    	this.skip = count;
        return this;
    }
    
    /**
     * 查询总条数
     * @return
     */
    public WhereCondition<T> count(){
    	this.count = "COUNT(1) as count";
    	return this;
    }

    /**
     * 统计某一列的不为null的条数
     * @return
     */
    public WhereCondition<T> count(MethodRefWith1Arg<T> fieldGetterMethod){
        if (null == fieldGetterMethod) {
            throw new NullArgumentException("fieldGetterMethod");
        }

        String columnName = getColumnName(fieldGetterMethod);
        if (StringUtils.isBlank(columnName)) {
            throw new IllegalArgumentException("fieldGetterMethod is null or empty");
        }

        this.count = "COUNT(`"+this.tableName+"`.`"+columnName+"`) AS count";

        return this;
    }
    


    private WhereCondition<T> orderBy(String type, MethodRefWith1Arg<T> fieldGetterMethod,
            MethodRefWith1Arg<T>... otherFieldGetterMethods) {
        if (null == fieldGetterMethod) {
            throw new NullArgumentException("fieldGetterMethod");
        }

        String columnName = getColumnName(fieldGetterMethod);
        if (StringUtils.isBlank(columnName)) {
            throw new IllegalArgumentException("fieldGetterMethod is null or empty");
        }
        columnName = "@" + columnName;

        String[] otherColumnNames = null;
        if (null != otherFieldGetterMethods) {
            otherColumnNames = new String[otherFieldGetterMethods.length];
            for (int i = 0; i < otherFieldGetterMethods.length; i++) {
                otherColumnNames[i] ="@" + getColumnName(otherFieldGetterMethods[i]);
            }
        }

        return orderBy(type, columnName, otherColumnNames);
    }

    private WhereCondition<T> orderBy(String type, String fieldName, String[] otherFieldNames) {
        if (StringUtils.isBlank(fieldName)) {
            throw new NullArgumentException("fieldName");
        }

        if (StringUtils.isBlank(type)) {
            throw new NullArgumentException("type");
        }

        List<String> list = new ArrayList<>();
        list.add(fieldName);
        if (null != otherFieldNames && otherFieldNames.length > 0) {
            for (String f : otherFieldNames) {
                list.add(f);
            }
        }

        setOrderBy(type, list);

        return this;
    }

    private void setOrderBy(String type, List<String> fieldNames) {
        List<String> list = new ArrayList<>();
        for (String fieldName : fieldNames) {
            String columnName = getColumnName(fieldName);
            if (null != columnName && !list.contains(columnName)) {
                list.add("`" + this.tableName + "`.`" + columnName + "`");
            }
        }

        this.orderBy = " ORDER BY " + String.join(",", list) + " " + type;
    }

    private String getColumnName(String fieldName) {
        if (null == fieldName) {
            throw new NullArgumentException("fieldName");
        }

        if (fieldName.startsWith("@")) {
            return fieldName.substring(1);
        }

        Method[] methods = cls.getMethods();

        for (Method m : methods) {
            if (null == m) {
                continue;
            }

            if (fieldName.equals(m.getName()) || fieldName.equals(Utils.getFieldName(m))) {
                if (null == m.getAnnotation(Column.class)) {
                    continue;
                }

                return Utils.getColumnName(m);
            }
        }

        return null;
    }
    
    public void clearCount() {
    	this.count = null;
    }
    
    public void clearOrderBy() {
    	this.orderBy = null;
    }
    
    public void clearSkip(){
        this.skip = null;
    }

    public void clearLimit() {
    	this.limit = null;
    }
    
    

}
