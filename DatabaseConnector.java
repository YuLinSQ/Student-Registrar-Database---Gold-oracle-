import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import oracle.jdbc.pool.OracleDataSource;
import oracle.jdbc.OracleConnection;
import java.sql.DatabaseMetaData;

public class DatabaseConnector {
    private static final String DB_URL = "jdbc:oracle:thin:@cs174aproj_low?TNS_ADMIN=./Wallet_CS174aProj";
    private static final String DB_USER = "ADMIN";
    private static final String DB_PASSWORD = "lilyPad.2460";
    
    public static Connection connect() throws SQLException {
        try {
            // set up connection
            Properties info = new Properties();
            info.put(OracleConnection.CONNECTION_PROPERTY_USER_NAME, DB_USER);
            info.put(OracleConnection.CONNECTION_PROPERTY_PASSWORD, DB_PASSWORD);
            info.put(OracleConnection.CONNECTION_PROPERTY_DEFAULT_ROW_PREFETCH, "20");

            // create and configure OracleDataSource
            OracleDataSource ods = new OracleDataSource();
            ods.setURL(DB_URL);
            ods.setConnectionProperties(info);

            // get connection
            OracleConnection connection = (OracleConnection) ods.getConnection();
            
            // print connection details
            DatabaseMetaData dbmd = connection.getMetaData();
            System.out.println("Driver Name: " + dbmd.getDriverName());
            System.out.println("Driver Version: " + dbmd.getDriverVersion());
            System.out.println("Default Row Prefetch Value: " + connection.getDefaultRowPrefetch());
            System.out.println("Database username: " + connection.getUserName());
            
            return connection;
        } 
        
        catch (SQLException e) {
            throw new SQLException("Failed to establish database connection", e);
        }
    }
} 