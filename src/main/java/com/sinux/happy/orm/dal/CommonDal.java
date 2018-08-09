package com.sinux.happy.orm.dal;

import com.sinux.happy.orm.exceptions.IllegalSqlCommandException;
import com.sinux.happy.orm.exceptions.TooManyResultsException;
import org.apache.commons.lang.NullArgumentException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于JDBC的数据库访问层
 *
 * @author lihp
 * @author zhaosh
 * @since 2018-07-20
 */
public class CommonDal {

    public static Transaction withTransaction() throws SQLException {
        return new Transaction(JdbcUtils.getConnection());
    }

    // #region createConnection
    public static Connection createConnection(String dbType, String server, String dbName, String username,
                                              String password, Map<String, String> options) throws SQLException {
        String url = String.format("jdbc:%s://%s/%s", dbType, server, dbName);
        if (null != options && !options.isEmpty()) {
            List<String> optList = new ArrayList<>();
            for (String key : options.keySet()) {
                optList.add(key + "=" + options.get(key));
            }

            String opt = String.join("&", optList);
            if (opt.trim().length() > 0) {
                url = url + "?" + opt;
            }
        }

        return DriverManager.getConnection(url, username, password);
    }

    public static Connection createConnection(String dbType, String server, String dbName, String username,
                                              String password, String... options) throws SQLException {
        String url = String.format("jdbc:%s://%s/%s", dbType, server, dbName);
        if (null != options) {
            String opt = String.join("&", options);
            if (opt.trim().length() > 0) {
                url = url + "?" + opt;
            }
        }

        return DriverManager.getConnection(url, username, password);
    }
    // #endregion createConnection

    // #region execNonQuery

    /**
     * 通过JDBC执行一句SQL
     *
     * @param sql
     */
    public static boolean execNonQuery(Connection con, String sql) throws IllegalSqlCommandException {
        return execNonQuery(con, sql, null);
    }

    public static boolean execNonQuery(Connection con, String sql, Object parameter) throws IllegalSqlCommandException {
        if (null == sql || sql.trim().toUpperCase().startsWith("SELECT")) {
            throw new IllegalSqlCommandException("错误的non-query SQL");
        }

        Object r = JdbcUtils.execSQL(con, sql, parameter);
        if (null == r) {
            return false;
        }

        if (r instanceof Boolean) {
            return ((Boolean) r).booleanValue();
        }

        return false;
    }

    public static boolean execNonQuery(String sql) throws IllegalSqlCommandException {
        return execNonQuery(sql, null);
    }

    public static boolean execNonQuery(String sql, Object parameter) throws IllegalSqlCommandException {
        return execNonQuery(null, sql, parameter);
    }

    public static boolean execNonQuery(Sql sql) throws IllegalSqlCommandException {
        if (null == sql) {
            throw new NullArgumentException("sql");
        }

        return execNonQuery(sql.getSql(), sql.getParamters());
    }

    public static boolean execNonQuery(Connection connection, Sql sql) throws IllegalSqlCommandException {
        if (null == sql) {
            throw new NullArgumentException("sql");
        }

        return execNonQuery(connection, sql.getSql(), sql.getParamters());
    }

    // #endregion execNonQuery

    // #region execQueryWithSingleResult

    public static <T> Object execQueryWithSingleResult(Connection connection, Sql<T> sql) throws IllegalSqlCommandException {
        return execQueryWithSingleResult(connection, sql.getSql(), sql.getParamters());
    }

    public static Object execQueryWithSingleResult(Connection connection, String sql) throws IllegalSqlCommandException {
        return execQueryWithSingleResult(connection, sql, null);
    }

    public static <T> Object execQueryWithSingleResult(Sql<T> sql) throws IllegalSqlCommandException {
        return execQueryWithSingleResult(null, sql.getSql(), sql.getParamters());
    }

    public static Object execQueryWithSingleResult(String sql) throws IllegalSqlCommandException {
        return execQueryWithSingleResult(null, sql, null);
    }

    public static Object execQueryWithSingleResult(String sql, Object parameter) throws IllegalSqlCommandException {
        return execQueryWithSingleResult(null, sql, parameter);
    }

    public static Object execQueryWithSingleResult(Connection connection, String sql, Object parameter) throws IllegalSqlCommandException {

        try {
            Object o = JdbcUtils.execSQL(connection, sql, parameter);

            if (o instanceof List) {
                if (((List) o).size() > 1) {
                    throw new TooManyResultsException("exeQueryWithSingleReuslt 要求返回结果只能为一个"); // 查询结果不止一个
                } else if (((List) o).size() == 0) {
                    return null;
                }

                Map<String, Object> map = (Map<String, Object>) ((List) o).get(0);

                if (map.values().size() > 1) {
                    throw new TooManyResultsException("exeQueryWithSingleReuslt 要求返回结果只能为一个"); // 查询结果不止一个
                }

                for (Object key : map.keySet()) {
                    return  map.get(key);
                }
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalSqlCommandException("错误的SQL" + sql);
        }

        return null;
    }

    public static <T> QueryResult<T> execQuery(Sql<T> sql) throws IllegalSqlCommandException {
        return execQuery(null, sql.getResultType(), sql.getSql(), sql.getParamters());
    }

    public static <T> QueryResult<T> execQuery(Class<T> clazz, String sql) throws IllegalSqlCommandException {
        return execQuery(null, clazz, sql, null);
    }

    public static <T> QueryResult<T> execQuery(Class<T> clazz, String sql, Object parameter) throws IllegalSqlCommandException {
        return execQuery(null, clazz, sql, parameter);
    }

    public static <T> QueryResult<T> execQuery(Connection con, Class<T> clazz, String sql) throws IllegalSqlCommandException {
        return execQuery(con, clazz, sql, null);
    }

    public static <T> QueryResult<T> execQuery(Connection con, Class<T> clazz, String sql, Object parameter)
            throws IllegalSqlCommandException {
        if (null == sql || !sql.trim().toUpperCase().startsWith("SELECT ")) {
            throw new IllegalSqlCommandException("SQL Query 必须以SELECT开头");
        }

        Object r = JdbcUtils.execSQL(con, sql, parameter);
        if (null == r) {
            return new QueryResult<>(clazz, null);
        }

        if (r instanceof List) {
            List<Map<String, Object>> list = (List<Map<String, Object>>) r;
            return new QueryResult<>(clazz, list);
        }

        return new QueryResult<>(clazz, null);
    }
}
