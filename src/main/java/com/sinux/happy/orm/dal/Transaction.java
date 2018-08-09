package com.sinux.happy.orm.dal;

import com.sinux.happy.orm.common.Logger;
import com.sinux.happy.orm.common.ThrowingConsumer;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 在JDBC中执行事务
 *
 * @author zhaosh
 * @since 2018-07-20
 */
public class Transaction {

    private final Connection con;

    public Transaction(Connection connection)
            throws SQLException {
        this.con = connection;
        this.con.setAutoCommit(false);
    }

    public Transaction then(ThrowingConsumer<Connection> action) {
        try {
            action.accept(con);

            return this;
        } catch (Exception e) {
            try {
                con.rollback();
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException ex) {
                        Logger.logError("SQLException", ex);
                    }
                }
            } catch (SQLException e1) {
                Logger.logError(e1);
            }

            throw e;
        }
    }

    public boolean commit() {
        try {
            this.con.commit();

            return true;
        } catch (Exception ex) {
            Logger.logError(ex);
            try {
                con.close();
            } catch (SQLException e) {
                Logger.logError(e);
            }
        }

        return false;
    }

}
