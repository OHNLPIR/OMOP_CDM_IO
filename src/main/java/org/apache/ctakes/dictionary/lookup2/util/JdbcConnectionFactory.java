//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.apache.ctakes.dictionary.lookup2.util;

import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.log4j.Logger;

public enum JdbcConnectionFactory {
    INSTANCE;

    public static final String JDBC_DRIVER = "jdbcDriver";
    public static final String JDBC_URL = "jdbcUrl";
    public static final String JDBC_USER = "jdbcUser";
    public static final String JDBC_PASS = "jdbcPass";
    private static final Logger LOGGER = Logger.getLogger("JdbcConnectionFactory");
    private static final Logger DOT_LOGGER = Logger.getLogger("ProgressAppender");
    private static final Logger EOL_LOGGER = Logger.getLogger("ProgressDone");
    private static final String HSQL_FILE_PREFIX = "jdbc:hsqldb:file:";
    private static final String HSQL_DB_EXT = ".script";
    private static final Map<String, Connection> CONNECTIONS = Collections.synchronizedMap(new HashMap());

    private JdbcConnectionFactory() {
    }

    public static JdbcConnectionFactory getInstance() {
        return INSTANCE;
    }

    public Connection getConnection(String jdbcDriver, String jdbcUrl, String jdbcUser, String jdbcPass) throws SQLException {
        Connection connection = (Connection)CONNECTIONS.get(jdbcUrl);
        if (connection != null) {
            return connection;
        } else {
            String trueJdbcUrl = jdbcUrl;
            if (jdbcUrl.startsWith("jdbc:hsqldb:file:")) {
                trueJdbcUrl = "jdbc:hsqldb:file:" + getConnectionUrl(jdbcUrl);
            }

            try {
                Driver driver = (Driver)Class.forName(jdbcDriver).newInstance();
                DriverManager.registerDriver(driver);
            } catch (SQLException var10) {
                LOGGER.error("Could not register Driver " + jdbcDriver, var10);
                throw var10;
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException var11) {
                LOGGER.error("Could not create Driver " + jdbcDriver, var11);
                throw new SQLException(var11);
            }

            LOGGER.info("Connecting to " + jdbcUrl + ":");
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new JdbcConnectionFactory.DotPlotter(), 333L, 333L);

            try {
                connection = DriverManager.getConnection(trueJdbcUrl, jdbcUser, jdbcPass);
            } catch (SQLException var9) {
                timer.cancel();
                EOL_LOGGER.error("");
                LOGGER.error("  Could not create Connection with " + trueJdbcUrl + " as " + jdbcUser, var9);
                throw var9;
            }

            timer.cancel();
            EOL_LOGGER.info("");
            LOGGER.info(" Database connected");
            this.CONNECTIONS.put(jdbcUrl, connection);
            return connection;
        }
    }

    private static String getConnectionUrl(String jdbcUrl) throws SQLException {
        String urlDbPath = jdbcUrl.substring("jdbc:hsqldb:file:".length());
        String urlFilePath = urlDbPath + ".script";

        try {
            String fullPath = FileLocator.getFullPath(urlFilePath);
            return fullPath.substring(0, fullPath.length() - ".script".length());
        } catch (FileNotFoundException var4) {
            throw new SQLException("No Hsql DB exists at Url", var4);
        }
    }

    private static class DotPlotter extends TimerTask {
        private int _count;

        private DotPlotter() {
            this._count = 0;
        }

        public void run() {
            JdbcConnectionFactory.DOT_LOGGER.info(".");
            ++this._count;
            if (this._count % 50 == 0) {
                JdbcConnectionFactory.EOL_LOGGER.info(" " + this._count);
            }

        }
    }
}
