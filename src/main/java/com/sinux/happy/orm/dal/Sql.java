package com.sinux.happy.orm.dal;

import java.util.HashMap;
import java.util.Map;

/**
 * 用來包裝SQl语句和SQL参数
 *
 * @author zhaosh
 * @since 2018-07-20
 */
public class Sql<T> {
    private String sql;
    private Map<String, Object> paramters;
    private Class<T> resultType;

    public Sql(Class<T> clazz) {
        this.setResultType(clazz);
    }

    /**
     * @return the cls
     */
    public Class<T> getResultType() {
        return resultType;
    }

    /**
     * @param cls the cls to set
     */
    public void setResultType(Class<T> cls) {
        this.resultType = cls;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public Map<String, Object> getParamters() {
        if (null == this.paramters) {
            this.paramters = new HashMap<>();
        }
        return paramters;
    }

    public void setParamters(Map<String, Object> paramters) {
        this.paramters = paramters;
    }
}
