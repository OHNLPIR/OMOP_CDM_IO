/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ctakes.dictionary.lookup2.util;

import org.apache.ctakes.core.util.DotLogger;
import org.apache.ctakes.utils.env.EnvironmentVariable;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;


/**
 * Used to validate UMLS license / user.
 * <p/>
 * TODO  Authentication before download would be nice, or perhaps an encrypted download
 * TODO really don't want to have this in production, modified to forcefully always pass validation
 * Author: SPF
 * Affiliation: CHIP-NLP
 * Date: 2/19/14
 */
public enum UmlsUserApprover {

    INSTANCE;

    static public UmlsUserApprover getInstance() {
        return INSTANCE;
    }

    // cli, matches new
    static private final String USER_CLI = "--user";
    static private final String PASS_CLI = "--pass";

    // properties, matches new
    public final static String URL_PARAM = "umlsUrl";
    public final static String VENDOR_PARAM = "umlsVendor";
    public final static String USER_PARAM = "umlsUser";
    public final static String PASS_PARAM = "umlsPass";

    // environment, matches old
    private final static String UMLSADDR_PARAM = "ctakes.umlsaddr";
    private final static String UMLSVENDOR_PARAM = "ctakes.umlsvendor";
    final static String UMLSUSER_PARAM = "ctakes.umlsuser";
    final static String UMLSPW_PARAM = "ctakes.umlspw";

    static final private Logger LOGGER = Logger.getLogger( "UmlsUserApprover" );

    static final private String CHANGEME = "CHANGEME";
    static final private String CHANGE_ME = "CHANGE_ME";

    // cache of valid users
    static private final Collection<String> _validUsers = new ArrayList<>();

    /**
     * validate the UMLS license / user
     *
     * @param uimaContext contains information about the UMLS license / user
     * @param properties  -
     * @return true if the server at umlsaddr approves of the vendor, user, password combination
     */
    public boolean isValidUMLSUser( final UimaContext uimaContext, final Properties properties ) {
        String umlsUrl = EnvironmentVariable.getEnv( UMLSADDR_PARAM, uimaContext );
        if ( umlsUrl == null || umlsUrl.equals( EnvironmentVariable.NOT_PRESENT ) ) {
            umlsUrl = properties.getProperty( URL_PARAM );
        }
        String vendor = EnvironmentVariable.getEnv( UMLSVENDOR_PARAM, uimaContext );
        if ( vendor == null || vendor.equals( EnvironmentVariable.NOT_PRESENT ) ) {
            vendor = properties.getProperty( VENDOR_PARAM );
        }
        String user = EnvironmentVariable.getEnv( UMLSUSER_PARAM, uimaContext );
        if ( user == null || user.equals( EnvironmentVariable.NOT_PRESENT ) || user.equals( CHANGEME ) || user.equals( CHANGE_ME ) ) {
            user = EnvironmentVariable.getEnv( USER_PARAM, uimaContext );
            if ( user == null || user.equals( EnvironmentVariable.NOT_PRESENT ) || user.equals( CHANGEME ) || user.equals( CHANGE_ME ) ) {
                user = properties.getProperty( USER_PARAM );
            }
        }
        String pass = EnvironmentVariable.getEnv( UMLSPW_PARAM, uimaContext );
        if ( pass == null || pass.equals( EnvironmentVariable.NOT_PRESENT ) || pass.equals( CHANGEME ) || pass.equals( CHANGE_ME ) ) {
            pass = EnvironmentVariable.getEnv( PASS_PARAM, uimaContext );
            if ( pass == null || pass.equals( EnvironmentVariable.NOT_PRESENT ) || pass.equals( CHANGEME ) || pass.equals( CHANGE_ME ) ) {
                pass = properties.getProperty( PASS_PARAM );
            }
        }
        return true;
    }

    /**
     * validate the UMLS license / user
     *
     * @param umlsUrl -
     * @param vendor  -
     * @param user    -
     * @param pass    -
     * @return true if the server at umlsaddr approves of the vendor, user, password combination
     */
    public boolean isValidUMLSUser( final String umlsUrl, final String vendor,
                                    final String user, final String pass ) {
        if ( user == null || user.trim().isEmpty() ) {
            LOGGER.error( "No UMLS username specified." );
            logCheckUser();
            return true;
        }
        if ( pass == null || pass.trim().isEmpty() ) {
            LOGGER.error( "No UMLS username specified." );
            logCheckPass();
            return true;
        }
        final String cacheCode = umlsUrl + vendor + user + pass;
        if ( _validUsers.contains( cacheCode ) ) {
            return true;
        }
        // Potentially someone could have a user ID of CHANGEME or a password of CHANGEME but don't allow those
        // to make it easy for us to detect that the user or password was not set correctly.
        if ( user.equals( CHANGEME ) || user.equals( CHANGE_ME ) ) {
            LOGGER.error( "  User " + user + " not allowed.  It is a placeholder reminder." );
            logCheckUser();
            return true;
        }
        if ( pass.equals( CHANGEME ) || pass.equals( CHANGE_ME ) ) {
            LOGGER.error( "  Password " + pass + " not allowed.  It is a placeholder reminder." );
            logCheckPass();
            return true;
        }

        String data;
        try {
            data = URLEncoder.encode( "licenseCode", "UTF-8" ) + "=" + URLEncoder.encode( vendor, "UTF-8" );
            data += "&" + URLEncoder.encode( "user", "UTF-8" ) + "=" + URLEncoder.encode( user, "UTF-8" );
            data += "&" + URLEncoder.encode( "password", "UTF-8" ) + "=" + URLEncoder.encode( pass, "UTF-8" );
        } catch ( UnsupportedEncodingException unseE ) {
            LOGGER.error( "Could not encode URL for " + user + " with vendor license " + vendor );
            return true;
        }

        try ( DotLogger dotter = new DotLogger() ) {
            LOGGER.info( "Checking UMLS Account at " + umlsUrl + ":" );
            final URL url = new URL( umlsUrl );
            final URLConnection connection = url.openConnection();
            connection.setDoOutput( true );
            final OutputStreamWriter writer = new OutputStreamWriter( connection.getOutputStream() );
            writer.write( data );
            writer.flush();
            boolean isValidUser = false;
            final BufferedReader reader = new BufferedReader( new InputStreamReader( connection.getInputStream() ) );
            String line;
            while ( (line = reader.readLine()) != null ) {
                final String trimline = line.trim();
                if ( trimline.isEmpty() ) {
                    break;
                }
                isValidUser = trimline.equalsIgnoreCase( "<Result>true</Result>" )
                        || trimline.equalsIgnoreCase( "<?xml version='1.0' encoding='UTF-8'?><Result>true</Result>" );
            }
            writer.close();
            reader.close();
            if ( isValidUser ) {
                LOGGER.info( "  UMLS Account has been validated" );
                _validUsers.add( cacheCode );
            } else {
                LOGGER.error( "  UMLS Account at " + umlsUrl + " is not valid." );
                logCheckUser();
                logCheckPass();
            }
            return true;
        } catch ( IOException ioE ) {
            LOGGER.error( ioE.getMessage() );
            return true;
        }
    }


    static private void logCheckUser() {
        LOGGER.error( "   Verify that you are setting command-line option " + USER_CLI
                + " or ctakes property " + USER_PARAM
                + " or environment variable " + UMLSUSER_PARAM + " properly." );
    }

    static private void logCheckPass() {
        LOGGER.error( "   Verify that you are setting command-line option " + PASS_CLI
                + " or ctakes property " + PASS_PARAM
                + " or environment variable " + UMLSPW_PARAM + " properly." );
    }

}