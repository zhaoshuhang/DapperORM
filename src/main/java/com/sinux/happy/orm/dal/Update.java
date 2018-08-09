package com.sinux.happy.orm.dal;

import com.sinux.happy.orm.annotation.CanUpdate;
import com.sinux.happy.orm.annotation.Column;
import com.sinux.happy.orm.annotation.Table;
import com.sinux.happy.orm.annotation.VerifyOnUpdate;
import com.sinux.happy.orm.common.Utils;
import com.sinux.happy.orm.exceptions.AnnotationMissingException;
import com.sinux.happy.orm.exceptions.TableNotFoundException;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sinux.happy.orm.common.Utils.getValueByName;

/**
 * 用于构造UPDATE的SQL语句
 *
 * @author lihp
 * @author zhaosh
 * @since 2018-07-20
 */
public class Update<T> extends WhereCondition<T> {
    private T data;

    public Update(T data) {
        super((Class<T>) data.getClass());
        this.data = data;
    }

    @Override
    public Sql<T> toSql() {
        Table table = data.getClass().getAnnotation(Table.class);
        if (null == table || StringUtils.isBlank(table.value())) {
            throw new TableNotFoundException(data.getClass().getName());
        }
        List<String> columns = new ArrayList<>();
        Map<String, Object> parameters = new HashMap<>();
        for (Method method : data.getClass().getMethods()) {
            CanUpdate canUpdate = method.getAnnotation(CanUpdate.class);
            // 未配置注解，不需要update
            if (null == canUpdate) {
                continue;
            }

            Column column = method.getAnnotation(Column.class);
            if (null == column) {
                throw new IllegalArgumentException("标记CanUpdate的注解必须标记为Column");
            }

            Object value = getValueByName(method, data);
            if (null == value) {
                continue;
            }

            String columnName = Utils.getColumnName(method, column.value());
            parameters.put(columnName, value);
            columns.add(String.format("`%1$s` = #{%1$s}", columnName));
        }

        Map<String, Object> updateCondition = getUpdateConditions(data);
        if (updateCondition.isEmpty()) {
            throw new AnnotationMissingException("UPDATE 要求实体必须至少有一个VerifyOnUpdate注解");
        }


        List<String> whereConditions = new ArrayList<>();
        for (String key : updateCondition.keySet()) {
            whereConditions.add(String.format("(`%1$s` = #{%1$s})", key));
            parameters.putIfAbsent(key, updateCondition.get(key));
        }

        String whereCondition = String.join(" AND ", whereConditions);

        Sql<T> sql = new Sql<>(this.getCls());
        sql.setSql(String.format("UPDATE `%s` SET %s WHERE %s",
                table.value(),
                String.join(",", columns),
                whereCondition));

        String otherWhereCondition = super.getWhereCondition();
        if (null != otherWhereCondition && otherWhereCondition.length() > 0) {
            sql.setSql(sql.getSql() + " AND " + otherWhereCondition);
        }

        sql.setParamters(parameters);

        return sql;
    }

    private <T> Map<String, Object> getUpdateConditions(T data) {
        Map<String, Object> parameters = new HashMap<>();
        for (Method method : data.getClass().getMethods()) {
            VerifyOnUpdate update = method.getAnnotation(VerifyOnUpdate.class);
            if (null == update) {
                continue;
            }

            Column column = method.getAnnotation(Column.class);

            // 未配置注解，不需要insert
            if (null == column) {
                continue;
            }

            Object value = getValueByName(method, data);
            if (null == value) {
                continue;
            }

            parameters.put(Utils.getColumnName(method, column.value()), value);
        }

        return parameters;
    }


}
