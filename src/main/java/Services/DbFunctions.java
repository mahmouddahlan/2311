package Services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbFunctions {

    // Private constructor to prevent instantiation.
    private DbFunctions() {}

    public static Connection connect() {
        // Assuming dbPath is your SQLite database path.
        String dbPath = "/Users/ahmedel-dib/Desktop/2311/2311-itr2/src/main/resources/movie.db";
        try {
            // Create a new connection to the database.
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            System.out.println("Connected to DB");
            return connection;
        } catch (SQLException e) {
            System.out.println("Connection failed: " + e.getMessage());
            return null;
        }
    }
}
