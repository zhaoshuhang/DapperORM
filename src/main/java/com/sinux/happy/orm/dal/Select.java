package com.sinux.happy.orm.dal;

import com.sinux.happy.orm.annotation.Column;
import com.sinux.happy.orm.annotation.ForeignKey;
import com.sinux.happy.orm.annotation.Table;
import com.sinux.happy.orm.common.Utils;
import org.apache.commons.lang.StringUtils;

import java.lang.annotation.AnnotationFormatError;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 用于构造SELECT的SQL语句
 *
 * @author lihp
 * @author zhaosh
 * @since 2018-07-20
 */
public class Select<T> extends WhereCondition<T> {


    protected Select(Class<T> clazz) {
        super(clazz);
    }

    private static String getColumnNameFromMethod(Class<?> clazz, String methodName) {
        Method[] methods = clazz.getMethods();
        for (Method m : methods) {
            if (m.getName().equals(methodName)) {
                Column column = m.getAnnotation(Column.class);
                if (null != column) {
                    return Utils.getColumnName(m, column.value());
                }
            }
        }

        return null;
    }

    /**
     * 获取返回类型为Entity的外键列
     *
     * @param returnType
     * @return
     */
    private static String returnTypeColumn(Class<?> returnType) {
        for (Method returnTypeMethod : returnType.getMethods()) {
            //Logger.logMessage("方法名：" + returnType.getName(), returnTypeMethod.getName());
            ForeignKey returnTypeColumn = returnTypeMethod.getAnnotation(ForeignKey.class);
            if (null == returnTypeColumn) {
                continue;
            }

            Column column = returnTypeMethod.getAnnotation(Column.class);
            if (null == column) {
                continue;
            }

            return Utils.getColumnName(returnTypeMethod, column.value());
        }
        return "";
    }

    /**
     * 级联对象select body
     *
     * @param cascadeMethod
     * @param customData    select body
     * @param joinAlias     join 别名
     */
    private static void getCascadeSelectColumns(Method cascadeMethod, List<String> customData, String joinAlias) {
        for (Method method : cascadeMethod.getReturnType().getMethods()) {
            Column column = method.getAnnotation(Column.class);
            if (null == column) {
                continue;
            }

            // 这里认为级联对象中的selectSql类型字段不在存在自定义级联对象
            String selectColumn = String.format("`%s`.`%s` AS `%s.%s`",
                    joinAlias,
                    Utils.getColumnName(method, column.value()),
                    Utils.getFieldName(cascadeMethod),
                    Utils.getFieldName(method));
            ;
            customData.add(selectColumn);
        }
    }

    @Override
    public Sql<T> toSql() {
        Table table = super.getCls().getAnnotation(Table.class);
        String tableName = table.value();
        if (null == table || StringUtils.isBlank(tableName)) {
            throw new AnnotationFormatError(super.getCls().getName() + "缺少@Table注解");
        }

        String customTable = tableName;
        
        // 关联查询
        List<String> customJoin = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT ");
        
    	if(StringUtils.isNotBlank(super.getCount())) {
            // 计数查询
    		sqlBuilder.append(super.getCount());
    	}else {
            // 需要查询的字段
            List<String> customData = new ArrayList<>();
            for (Method method : super.getCls().getMethods()) {
                singleColumnParam(customData, customJoin, method);
            }
    		sqlBuilder.append(String.join(",", customData));
    	}
        sqlBuilder.append(" FROM ");
        sqlBuilder.append(customTable);
        sqlBuilder.append(" ");
        sqlBuilder.append(String.join(" ", customJoin));
        String where = super.getWhereCondition();
        if (where.length() > 0) {
            sqlBuilder.append(" WHERE ");
            sqlBuilder.append(where);
        }

        sqlBuilder.append(" ");
        sqlBuilder.append(super.getOrderBy());
        sqlBuilder.append(" ");
        sqlBuilder.append(super.getLimit());
        Sql<T> sql = new Sql<>(this.getCls());
        sql.setSql(sqlBuilder.toString());

        return sql;
    }

    /**
     * 获取单个变量方法的定制语句
     *
     * @param customData select body
     * @param customJoin join body
     * @param method
     */
    private void singleColumnParam(
            List<String> customData, List<String> customJoin, Method method) {
        Column column = method.getAnnotation(Column.class);
        if (null == column) {
            return;
        }

        Class<?> returnType = method.getReturnType();
        Table joinTable = returnType.getAnnotation(Table.class);
        boolean isJoin = null != joinTable;
        if (isJoin) {
            String columnName = Utils.getColumnName(method, column.value());
            if (StringUtils.isBlank(joinTable.value())) {
                throw new IllegalArgumentException("Table缺少表名");
            }

            String joinTableAlias = "t" + UUID.randomUUID().toString().substring(0, 5);
            String columnForJoin = column.refField();
            if (StringUtils.isBlank(columnForJoin)) {
                if (StringUtils.isBlank(column.refColumnMethod())) {
                    columnForJoin = returnTypeColumn(returnType);
                } else {
                    columnForJoin = getColumnNameFromMethod(returnType, column.refColumnMethod());
                    if (StringUtils.isBlank(columnForJoin)) {
                        throw new IllegalArgumentException("找不到需要关联的字段");
                    }
                }
            }

            customJoin.add(
                    String.format(" LEFT JOIN `%s` %s ON %s.`%s` = `%s`.`%s`",
                            joinTable.value(),
                            joinTableAlias,
                            joinTableAlias,
                            columnForJoin,
                            super.getTableName(),
                            columnName));
            getCascadeSelectColumns(method, customData, joinTableAlias);
        } else {
            customData.add(getSelectSql(method, column));
        }
    }

    private String getSelectSql(Method method, Column column) {
        String columnName = Utils.getColumnName(method, column.value());
        String columnNameSql = String.format(" `%s`.`%s` AS `%s`",
                super.getTableName(),
                columnName,
                Utils.getFieldName(method));

        return columnNameSql;
    }
}
