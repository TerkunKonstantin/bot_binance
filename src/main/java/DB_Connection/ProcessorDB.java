package DB_Connection;

import com.mysql.cj.jdbc.Driver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;



public class ProcessorDB {
    private Connection connection;
    public ProcessorDB() throws SQLException{
        Driver driver = new Driver();
        DriverManager.registerDriver(driver);
    }

    public Connection getConnection(String url, String username, String password) throws SQLException {
        if(connection != null){
            return connection;
        }
        connection = DriverManager.getConnection(url, username,password);
        return connection;
    }
}
