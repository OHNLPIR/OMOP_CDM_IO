package org.apache.ctakes.dictionary.lookup2.util;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.apache.ctakes.core.resource.FileLocator;
import org.apache.log4j.Logger;

import javax.sql.DataSource;
import java.beans.PropertyVetoException;
import java.io.*;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Some JDBC Connections can be reused, for instance by a Dictionary and Concept Factory.
 * This Singleton keeps a map of JDBC URLs to open and reusable Connections
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/29/2014
 */
public enum JdbcConnectionFactory {
    INSTANCE;

    static public final String JDBC_DRIVER = "jdbcDriver";
    static public final String JDBC_URL = "jdbcUrl";
    static public final String JDBC_USER = "jdbcUser";
    static public final String JDBC_PASS = "jdbcPass";

    static final private Logger LOGGER = Logger.getLogger( "JdbcConnectionFactory" );
    static final private Logger DOT_LOGGER = Logger.getLogger( "ProgressAppender" );
    static final private Logger EOL_LOGGER = Logger.getLogger( "ProgressDone" );

    static private final String HSQL_FILE_PREFIX = "jdbc:hsqldb:file:";
    static private final String HSQL_DB_EXT = ".script";
    private final Map<String, DataSource> CONNECTION_POOLS = new ConcurrentHashMap<>();

    public static JdbcConnectionFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Get an existing Connection or create and store a new one
     *
     * @param jdbcDriver -
     * @param jdbcUrl    -
     * @param jdbcUser   -
     * @param jdbcPass   -
     * @return a previously opened or new Connection
     * @throws SQLException if a JDBC Driver could not be created or registered,
     *                      or if a Connection could not be made to the given <code>jdbcUrl</code>
     */
    public Connection getConnection( final String jdbcDriver,
                                     final String jdbcUrl,
                                     final String jdbcUser,
                                     final String jdbcPass ) throws SQLException {
        DataSource pool = CONNECTION_POOLS.get(jdbcUrl);
        if (pool != null) {
            return pool.getConnection();
        }
        String trueJdbcUrl = jdbcUrl;
        if ( jdbcUrl.startsWith( HSQL_FILE_PREFIX ) ) {
            // Hack for hsqldb file needing to be absolute or relative to current working directory
            trueJdbcUrl = HSQL_FILE_PREFIX + getConnectionUrl(jdbcUrl);
        }
        try {
            // DO NOT use try with resources here.
            // Try with resources uses a closable and closes it when exiting the try block
            final Driver driver = (Driver)Class.forName( jdbcDriver ).newInstance();
            DriverManager.registerDriver( driver );
        } catch ( SQLException sqlE ) {
            LOGGER.error( "Could not register Driver " + jdbcDriver, sqlE );
            throw sqlE;
        } catch ( ClassNotFoundException | InstantiationException | IllegalAccessException multE ) {
            LOGGER.error( "Could not create Driver " + jdbcDriver, multE );
            throw new SQLException( multE );
        }
//        File in = new File(getConnectionUrlPlain(jdbcUrl) + ".script");
//        File out = new File(in.getParent(), in.getName().replace(".script", "") + "." + id + ".script");
//        try {
//            InputStream is = new FileInputStream(in);
//            OutputStream os = new FileOutputStream(out);
//            byte[] buf = new byte[is.available()];
//            is.read(buf);
//            os.write(buf);
//            is.close();
//            os.flush();
//            os.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        LOGGER.info( "Connecting to " + trueJdbcUrl);
        final Timer timer = new Timer();
        timer.scheduleAtFixedRate( new DotPlotter(), 333, 333 );
        try {
            // DO NOT use try with resources here.
            // Try with resources uses a closable and closes it when exiting the try block
            // We need the Connection later, and if it is closed then it is useless
            ComboPooledDataSource cpds = new ComboPooledDataSource();
            cpds.setDriverClass(jdbcDriver);
            cpds.setJdbcUrl(trueJdbcUrl);
            cpds.setUser(jdbcUser);
            cpds.setPassword(jdbcPass);
            cpds.setMaxPoolSize(Integer.MAX_VALUE - 1); // Unbounded pool
            pool = cpds;
        } catch ( PropertyVetoException e ) {
            timer.cancel();
            EOL_LOGGER.error( "" );
//            LOGGER.error( "  Could not create Connection with " + trueJdbcUrl + " as " + jdbcUser, sqlE );
            throw new SQLException(e); // Hacky way to wrap what cTAKES expects
        }
        timer.cancel();
        EOL_LOGGER.info( "" );
        LOGGER.info( " Database connected" );
        CONNECTION_POOLS.put(jdbcUrl, pool );
        return pool.getConnection();
    }

    /**
     * Uses {@link org.apache.ctakes.core.resource.FileLocator} to get the canonical path to the database file
     *
     * @param jdbcUrl -
     * @return -
     * @throws SQLException
     */
    static private String getConnectionUrl( final String jdbcUrl ) throws SQLException {
        return getConnectionUrlPlain(jdbcUrl);
    }

    private static String getConnectionUrlPlain(String jdbcUrl) throws SQLException {
        final String urlDbPath = jdbcUrl.substring( HSQL_FILE_PREFIX.length() );
        final String urlFilePath = urlDbPath + HSQL_DB_EXT;
        try {
            final String fullPath = FileLocator.getFullPath( urlFilePath );
            return fullPath.substring( 0, fullPath.length() - HSQL_DB_EXT.length() );
        } catch ( FileNotFoundException fnfE ) {
            throw new SQLException( "No Hsql DB exists at Url", fnfE );
        }
    }

    private static String getConnectionUrl(String jdbcUrl, long id) throws SQLException {
        final String urlDbPath = jdbcUrl.substring( HSQL_FILE_PREFIX.length() );
        final String urlFilePath = urlDbPath+ HSQL_DB_EXT;
        try {
            final String fullPath = FileLocator.getFullPath( urlFilePath );
            return fullPath.substring( 0, fullPath.length() - HSQL_DB_EXT.length() ) + "." + id ;
        } catch ( FileNotFoundException fnfE ) {
            throw new SQLException( "No Hsql DB exists at Url", fnfE );
        }
    }

    static private class DotPlotter extends TimerTask {
        private int _count = 0;

        @Override
        public void run() {
            DOT_LOGGER.info( "." );
            _count++;
            if ( _count % 50 == 0 ) {
                EOL_LOGGER.info( " " + _count );
            }
        }
    }

}