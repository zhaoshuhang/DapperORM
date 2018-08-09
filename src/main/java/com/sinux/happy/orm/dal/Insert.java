package com.sinux.happy.orm.dal;

import com.sinux.happy.orm.annotation.CanInsert;
import com.sinux.happy.orm.annotation.Column;
import com.sinux.happy.orm.annotation.Table;
import com.sinux.happy.orm.common.Utils;
import com.sinux.happy.orm.exceptions.AnnotationMissingException;
import com.sinux.happy.orm.exceptions.TableNotFoundException;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sinux.happy.orm.common.Utils.getValueByName;

/**
 * 用于构造INSERT的SQL语句
 *
 * @author zhaosh
 * @since 2018-07-20
 */
public class Insert<T> {
    Class<T> cls;
    T data;

    protected Insert(T data) {
        this.data = data;
        this.cls = (Class<T>) data.getClass();
    }

    private static <T> Map<String, Object> getParameters(T data) {
        Map<String, Object> columns = new HashMap<>();
        for (Method method : data.getClass().getMethods()) {
            CanInsert canInsert = method.getAnnotation(CanInsert.class);
            // 未配置注解，不需要insert
            if (null == canInsert) {
                continue;
            }

            Column column = method.getAnnotation(Column.class);
            if (null == column) {
                throw new IllegalArgumentException("标记CanInsert的注解必须标记为Column");
            }

            Object value = getValueByName(method, data);
            if (null == value) {
                continue;
            }
            columns.put(Utils.getColumnName(method, column.value()), value);
        }

        return columns;
    }

    protected Sql<T> toSql() {
        Table table = data.getClass().getAnnotation(Table.class);
        if (null == table || table.value().length() == 0) {
            throw new TableNotFoundException(data.getClass().getName());
        }

        Map<String, Object> parameters = getParameters(this.data);
        if (parameters.isEmpty()) {
            throw new AnnotationMissingException("需要Insert的实体必须至少有一个CanInsert注解");
        }

        List<String> keyList = new ArrayList<>();
        List<String> valueList = new ArrayList<>();
        for (String key : parameters.keySet()) {
            keyList.add("`" + key + "`");
            valueList.add("#{" + key + "}");
        }

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("INSERT INTO `");
        sqlBuilder.append(table.value().trim());
        sqlBuilder.append("` (");
        sqlBuilder.append(String.join(",", keyList));
        sqlBuilder.append(") VALUES (");
        sqlBuilder.append(String.join(",", valueList));
        sqlBuilder.append(")");

        Sql<T> sql = new Sql<>(this.cls);
        sql.setSql(sqlBuilder.toString());
        sql.setParamters(parameters);

        return sql;
    }
}