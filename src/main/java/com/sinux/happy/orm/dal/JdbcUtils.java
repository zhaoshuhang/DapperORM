package com.sinux.happy.orm.dal;

import com.sinux.happy.orm.common.Logger;
import com.sinux.happy.orm.common.ObjMap;
import com.sinux.happy.orm.common.Pair;
import com.sinux.happy.orm.common.RootPathHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;

import java.io.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class JdbcUtils {
    private static String _dbUrl = getPropertiesConfig("jdbc.url");
    private static String _username = getPropertiesConfig("jdbc.username");
    private static String _password = getPropertiesConfig("jdbc.password");

    public static Object execSQL(Connection con, String sql, Object parameter) {
        if (StringUtils.isBlank(sql)) {
            throw new IllegalArgumentException("sql为null");
        }

        sql.trim();
        String upperSql = sql.toUpperCase().replaceFirst("\\s+", " ");
        boolean isQuery = true;
        if (upperSql.startsWith("SELECT ")) {

        } else if (upperSql.startsWith("INSERT INTO ") || upperSql.startsWith("UPDATE ")
                || upperSql.startsWith("REPLACE ") || upperSql.startsWith("CREATE INDEX ")
                || upperSql.startsWith("DROP INDEX ") || upperSql.startsWith("DELETE FROM ")
                || upperSql.startsWith("ALTER TABLE ") || upperSql.startsWith("CREATE TABLE ")
                || upperSql.startsWith("DROP TABLE ")) {
            isQuery = false;
        } else {
            throw new IllegalArgumentException("错误的SQL命令：" + sql);
        }

        Logger.inst.info("执行SQL：" + sql);
        Statement stmt = null;
        // 是否释放连接对象
        boolean disposeConnection = false;
        if (null == con) {
            disposeConnection = true;
            con = getConnection();
        }

        try {
            ResultSet result = null;
            boolean isSuccess = false;
            if (null == parameter) {
                stmt = con.createStatement();
                if (isQuery) {
                    result = stmt.executeQuery(sql);
                } else {
                    isSuccess = stmt.execute(sql);
                }
            } else {
                PreparedStatement pstmt = getPreparedStatement(sql, new ObjMap(parameter), con);
                stmt = pstmt;
                if (isQuery) {
                    result = pstmt.executeQuery();
                } else {
                    isSuccess = pstmt.execute();
                }
            }

            if (isQuery) {
                return buildQueryResult(result);
            } else {
                return isSuccess;
            }
        } catch (SQLException ex) {
            Logger.logError("SQLException: " + ex.getMessage(), ex);
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    Logger.logError("SQLException: " + e.getMessage(), e);
                }
            }

            if (disposeConnection) {
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException e) {
                        Logger.logError("SQLException: " + e.getMessage(), e);
                    }
                }
            }
        }

        return null;
    }


    private static Object buildQueryResult(ResultSet rs) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            ResultSetMetaData rsmd = rs.getMetaData();
            int count = rsmd.getColumnCount();
            String[] names = new String[count];
            for (int i = 0; i < count; i++) {
                names[i] = rsmd.getColumnLabel(i + 1);
            }

            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                for (int i = 0; i < names.length; i += 1) {
                    Object v = rs.getObject(i + 1);
                    if (v instanceof Timestamp) {
                        map.put(names[i], new java.util.Date(((Timestamp) v).getTime()));
                    } else {
                        map.put(names[i], v);
                    }
                }
                result.add(map);
            }

            return result;
        } catch (Exception e) {
            Logger.logError(e);
        }

        return null;
    }

    private static PreparedStatement getPreparedStatement(String sql, ObjMap parameter, Connection con)
            throws SQLException {
        Pair<String, Map<Integer, Object>> sqlWithParameters = replaceSqlParameters(sql, parameter);
        String preparedSql = sqlWithParameters.getKey();
        Map<Integer, Object> parametersMap = sqlWithParameters.getValue();

        return getPreparedStatement(con, preparedSql, parametersMap);
    }

    private static PreparedStatement getPreparedStatement(Connection con, String prepairedSql,
                                                          Map<Integer, Object> parametersMap)
            throws SQLException {
        PreparedStatement pstmt = con.prepareStatement(prepairedSql);

        for (int i : parametersMap.keySet()) {
            Object o = parametersMap.get(i);
            if (null == o) {
                pstmt.setString(i, null);
            } else if (o instanceof String) {
                pstmt.setString(i, (String) o);
            } else if (o instanceof Integer) {
                pstmt.setInt(i, (Integer) o);
            } else if (o instanceof Long) {
                pstmt.setLong(i, (Long) o);
            } else if (o instanceof java.util.Date) {
                java.util.Date d = (java.util.Date) o;
                if (DateFormatUtils.format(d, "HH:mm:ss").equals("00:00:00")) {
                    pstmt.setDate(i, new java.sql.Date(d.getTime()));
                } else {
                    pstmt.setTimestamp(i, new Timestamp(d.getTime()));
                }
            } else if (o instanceof Boolean) {
                pstmt.setBoolean(i, (boolean) o);
            } else if (o instanceof Byte) {
                pstmt.setByte(i, (byte) o);
            } else if (o instanceof Byte[]) {
                pstmt.setBytes(i, (byte[]) o);
            } else if (o instanceof BigDecimal) {
                pstmt.setBigDecimal(i, (BigDecimal) o);
            } else if (o instanceof Double) {
                pstmt.setDouble(i, (double) o);
            } else if (o instanceof Float) {
                pstmt.setFloat(i, (float) o);
            } else {
                pstmt.setString(i, o.toString());
            }
        }
        return pstmt;
    }

    private static String getPropertiesConfig(String key) {
        // TODO: 要解決配置文件问题
        String path = new RootPathHelper().GetCurrentDir("sinuxOA.properties");
        File file = new File(path);
        if (!file.exists()) {
            path = new RootPathHelper().GetCurrentDir("db.properties");
            file = new File(path);
            if (!file.exists()) {
                Logger.logError("找不到" + path, null);
                return null;
            }
        }
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(path), "utf-8");
        } catch (UnsupportedEncodingException e) {
            Logger.logError(e);
            return null;
        } catch (FileNotFoundException e) {
            Logger.logError(e);
            return null;
        }

        Properties p = new Properties();
        try {
            p.load(reader);
            String val = p.getProperty(key);
            Logger.inst.info(String.format("读取配置文件%s=%s", key, val));

            return val;
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        return null;
    }

    public static Connection getConnection() {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(_dbUrl, _username, _password);
        } catch (Exception e) {
            Logger.logError(e);
        }
        return connection;
    }

    private static Pair<String, Map<Integer, Object>> replaceSqlParameters(String sql, ObjMap parameter) {
        StringBuilder sb = new StringBuilder();
        Pattern r = Pattern.compile("[#$]\\{[\\w\\.\\$]+?}", Pattern.CASE_INSENSITIVE + Pattern.MULTILINE);
        Matcher m = r.matcher(sql);
        Map<Integer, Object> paramMap = new HashMap<>();
        int matchOffset = 0;
        int parameterIndex = 1; // SQL的参数编号是从1开始的
        while (m.find(matchOffset)) {
            String match = m.group(0);
            boolean isSqlParameter = match.startsWith("#");
            String variable = match.substring(2, match.length() - 1).trim();
            Object value = parameter.get(variable);

            if (isSqlParameter) {
                // 如果是以SQL参数的形式
                sb.append(sql, matchOffset, m.start());
                sb.append("?");
                paramMap.put(parameterIndex, value);
                parameterIndex += 1;
            } else {
                // 直接替换SQL语句
                value = (null == value) ? "" : value.toString();
                sb.append(sql, matchOffset, m.start());
                sb.append(value);
            }

            matchOffset = m.end();
        }

        if (matchOffset < sql.length()) {
            sb.append(sql.substring(matchOffset));
        }

        return new Pair<>(sb.toString(), paramMap);
    }

}