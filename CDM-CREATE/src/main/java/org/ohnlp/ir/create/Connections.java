package org.ohnlp.ir.create;

import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class Connections {
    private static DataSource JDBC_DATA_SOURCE;

    static {
        if (!new File("CREATe.sqlite").exists()) {
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
                if (conn != null) {
                    conn.getMetaData(); // Trigger a db creation
                    conn.createStatement().execute("CREATE TABLE RESULTS_ENTRY (ENTRY_FK INTEGER PRIMARY KEY AUTOINCREMENT, RESULTS_FK INTEGER, DOCID VARCHAR(255), RELEVANCE_JUDGMENT INTEGER, JUDGEMENT_TYPE BOOLEAN, FOREIGN KEY(RESULTS_FK) REFERENCES RESULTS_LINK(RESULTS_FK))");
                    conn.createStatement().execute("CREATE TABLE RESULTS_LINK (RESULTS_FK INTEGER PRIMARY KEY AUTOINCREMENT)");
                    conn.createStatement().execute( "CREATE TABLE QUERYS (QUERY_FK INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "QUERY_NAME VARCHAR(255), " +
                            "UNSTRUCTURED_QUERY VARCHAR(255), " +
                            "STRUCTURED_QUERY VARCHAR(255), " +
                            "CDM_QUERY VARCHAR(255))");
                    conn.createStatement().execute("CREATE TABLE SAVED_STATES (STATE_FK INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "USER VARCHAR(255), " +
                            "QUERY_FK INTEGER, " +
                            "RESULTS_FK INTEGER, " +
                            "FOREIGN KEY(RESULTS_FK) REFERENCES RESULTS_LINK(RESULTS_FK)," +
                            "FOREIGN KEY(QUERY_FK) REFERENCES QUERYS(QUERY_FK))");

                    // - Index for performance
                    conn.createStatement().executeUpdate("CREATE INDEX USER_INDEX ON SAVED_STATES (USER)");
                    conn.createStatement().executeUpdate("CREATE INDEX ENTRY_INDEX ON RESULTS_ENTRY (ENTRY_FK)");
                    conn.createStatement().executeUpdate("CREATE INDEX ENTRY_CLEANUP_INDEX ON RESULTS_ENTRY (RESULTS_FK)");
                    conn.createStatement().executeUpdate("CREATE INDEX LOOKUP_INDEX ON SAVED_STATES (USER, QUERY_FK)");
                    conn.createStatement().executeUpdate("CREATE INDEX STATE_CLEANUP_INDEX ON SAVED_STATES (USER, QUERY_FK, RESULTS_FK)");
                    // - Write In-Memory DB To File
                    conn.createStatement().execute("backup to \"" + System.getProperty("user.dir").replace('\\', '/') + "/CREATe.sqlite\"");
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        String url = "jdbc:sqlite:CREATe.sqlite";
        SQLiteDataSource sqLiteDataSource = new SQLiteDataSource();
        sqLiteDataSource.setUrl(url);
        Map<Object, Object> overrides = new HashMap<>();
        overrides.put("maxPoolSize", 10);
        overrides.put("maxStatements", 20);
        JDBC_DATA_SOURCE = sqLiteDataSource;
//        try {
//            JDBC_DATA_SOURCE = DataSources.pooledDataSource(sqLiteDataSource, overrides);
//        } catch (SQLException e) {
//            e.printStackTrace();
//            System.exit(1);
//        }
    }

    public static Connection getConnection() throws SQLException {
        return JDBC_DATA_SOURCE.getConnection();
    }

    public static void main(String... args) throws SQLException {
        getConnection();
    }
}
