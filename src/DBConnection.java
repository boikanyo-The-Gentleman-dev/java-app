import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Credentials are NOT stored in code. Provide them either as environment variables
 * (PIMS_DB_URL, PIMS_DB_USER, PIMS_DB_PASSWORD) or in a config.properties file
 * next to the project (see config.properties.example). Do not commit that file.
 */
public class DBConnection {
    private static final Properties CONFIG = loadConfig();

    private static Properties loadConfig() {
        Properties p = new Properties();
        File f = new File("config.properties");
        if (f.exists()) {
            try (FileInputStream in = new FileInputStream(f)) {
                p.load(in);
            } catch (IOException ignored) { }
        }
        return p;
    }

    private static String setting(String key, String env, String fallback) {
        String v = System.getenv(env);
        if (v == null || v.isBlank()) v = CONFIG.getProperty(key);
        return (v == null || v.isBlank()) ? fallback : v.trim();
    }

    public static Connection getConnection() throws SQLException {
        String url = setting("db.url", "PIMS_DB_URL", "jdbc:mysql://localhost:3306/pims_db");
        String user = setting("db.user", "PIMS_DB_USER", "root");
        String pass = setting("db.password", "PIMS_DB_PASSWORD", null);
        if (pass == null) {
            throw new SQLException("Database password not configured. Set the PIMS_DB_PASSWORD environment "
                    + "variable or create config.properties (see config.properties.example).");
        }
        return DriverManager.getConnection(url, user, pass);
    }
}