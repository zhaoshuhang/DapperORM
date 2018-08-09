package com.sinux.happy.orm.common;

import com.sinux.happy.orm.annotation.Column;
import com.sinux.happy.orm.annotation.ForeignKey;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 提供一些通用方法
 *
 * @author zhaosh
 * @since 2018-07-20
 */
public class Utils {
    public static String getColumnName(Method method, String columnName) {
        if (StringUtils.isBlank(columnName)) {
            if (method.getName().length() > 3 &&
                    method.getName().startsWith("get")) {
                columnName = getFieldName(method);
            } else {
                throw new IllegalArgumentException("ColumnName为空");
            }
        }
        return columnName;
    }

    public static String getColumnName(Method method) {
        Column column = method.getAnnotation(Column.class);
        if (null == column) {
            return null;
        }

        return getColumnName(method, column.value());
    }

    /**
     * 获取对象属性get方法的属性名
     *
     * @return
     */
    public static String getFieldName(Method method) {
        if (null == method) {
            return null;
        }

        return getFieldName(method.getName());
    }

    public static boolean isBasicType(Object value) {
        return value instanceof BigDecimal ||
                value instanceof Float ||
                value instanceof Double ||
                value instanceof CharSequence ||
                value instanceof Boolean ||
                value instanceof Integer ||
                value instanceof Long ||
                value instanceof Number ||
                value instanceof Date;
    }

    public static String getFieldName(String methodName) {
        if (null == methodName || methodName.length() < 4) {
            return null;
        }

        if (methodName.startsWith("get") || methodName.startsWith("set")) {
            return methodName.substring(3, 4).toLowerCase() +
                    methodName.substring(4);
        }

        return null;
    }

    public static String convertObjToSqlString(Object value) {
        if (null == value) {
            return "null";
        }

        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue() ? "1" : "0";
        }

        if (value instanceof Date) {
            return "'" + DateFormatUtils.format((Date) value, "yyyy-MM-dd HH:mm:ss") + "'";
        }

        if (value instanceof Number) {
            return value.toString();
        }

        return "'" + value.toString().replaceAll("'", "\\'") + "'";
    }

    public static <T> Object getValueByName(Method method, T data) {
        Column column = method.getAnnotation(Column.class);
        // 未配置注解，不需要insert
        if (null == column) {
            return null;
        }

        // 若无返回值，不需要insert
        if (null == method.getReturnType()) {
            return null;
        }

        // 若为set方法，配置column注解无用
        if ("void".equals(method.getReturnType().getName())) {
            return null;
        }

        Object value = Reflections.invokeGetterByMethod(data, method.getName());
        if (null == value) {
            return null;
        }
//        System.err.println("动态保存参数方法名------------------"+method.getName()+"-----------"+value.toString());
        // 若非常用类型 ，则需要反射遍历此类方法是否有符合取值要求（例如关联外键的id）
        if (!isBasicType(value)) {
            for (Method childMethod : value.getClass().getMethods()) {
                ForeignKey childColumn = childMethod.getAnnotation(ForeignKey.class);
                // 未配置注解，不需要insert
                if (null == childColumn) {
                    continue;
                }

                // 若存在外键关系，抓取主表的外键id
                return getValueByName(childMethod, value);

            }

            return null;
        }
        return value;
    }
}
