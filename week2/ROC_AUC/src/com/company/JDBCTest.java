package com.company;

import java.sql.*;

/**
 * Created by hzhuangyixuan on 2017/2/28.
 */
public class JDBCTest {

    static{
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    private static final String DBURL = "jdbc:mysql://localhost:3306/roc_test";
    private static final String DBUSER = "test";
    private static final String PASSWORD = "123456kJ";



    public static void main(String[] args) {
        System.out.println("Connecting to database...");
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DBURL,DBUSER,PASSWORD);

            System.out.println("Creating statement...");
            Statement stmt = conn.createStatement();

            String sql;
            conn.res
            sql = "SELECT * FROM samples";
            ResultSet rs = stmt.executeQuery(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

}
