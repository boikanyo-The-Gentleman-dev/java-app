import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;

public class DBConnection {
    //URL for Local Mysql database
    private static  final String URL = "jdbc:mysql://localhost:3306/pims_db";
    //username
    private static final String USER = "root";
    //password
    private static final String PASSWORD = "Olebile@13$02$";

    public static Connection getConnection() throws SQLException {
        try {
            // Load MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found in project libraries!", e);
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}


