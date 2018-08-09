package com.sinux.happy.orm.dal;

import com.sinux.happy.orm.annotation.Column;
import com.sinux.happy.orm.annotation.Table;
import com.sinux.happy.orm.annotation.VerifyOnDelete;
import com.sinux.happy.orm.common.Utils;
import com.sinux.happy.orm.exceptions.AnnotationMissingException;
import com.sinux.happy.orm.exceptions.TableNotFoundException;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static com.sinux.happy.orm.common.Utils.getValueByName;

/**
 * 用于构造DELETE的SQL语句
 *
 * @author zhaosh
 * @since 2018-07-20
 */
public class Delete<T> extends WhereCondition<T> {
    private T data = null;

    public Delete(T data) {
        super((Class<T>) data.getClass());
        this.data = data;
    }

    public Delete(Class<T> clazz) {
        super(clazz);
    }

    private static <T> Map<String, Object> getParameters(T data) {
        Map<String, Object> columns = new HashMap<>();
        for (Method method : data.getClass().getMethods()) {
            VerifyOnDelete canInsert = method.getAnnotation(VerifyOnDelete.class);
            // 未配置注解，不需要insert
            if (null == canInsert) {
                continue;
            }

            Column column = method.getAnnotation(Column.class);
            if (null == column) {
                throw new IllegalArgumentException("标记VerifyOnDelete的注解必须标记为Column");
            }

            Object value = getValueByName(method, data);
            if (null == value) {
                continue;
            }
            columns.put(Utils.getColumnName(method, column.value()), value);
        }

        return columns;
    }

    @Override
    public Sql<T> toSql() {
        Table table = super.getCls().getAnnotation(Table.class);
        if (null == table || table.value().length() == 0) {
            throw new TableNotFoundException(data.getClass().getName());
        }

        Map<String, Object> parameters = null;
        if (null != this.data) {
            parameters = getParameters(this.data);
        }

        String whereSql = super.getWhereCondition();

        if (null == parameters && whereSql.length() == 0) {
            throw new AnnotationMissingException("Delete 缺少Where条件");
        }

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("DELETE FROM `");
        sqlBuilder.append(table.value());
        sqlBuilder.append("` WHERE (1=1 ");
        if (null != parameters) {
            for (String k : parameters.keySet()) {
                sqlBuilder.append(String.format(" AND (`%1$s`=#{%1$s}) ", k));
            }
        }
        sqlBuilder.append(") ");
        if (whereSql.length() > 0) {
            sqlBuilder.append(" AND ");
            sqlBuilder.append(whereSql);
        }

        sqlBuilder.append(super.getLimit());

        Sql<T> sql = new Sql<>(this.getCls());
        sql.setSql(sqlBuilder.toString());
        sql.setParamters(parameters);

        return sql;
    }
}
