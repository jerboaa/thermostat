/*
 * Copyright 2012-2017 Red Hat, Inc.
 *
 * This file is part of Thermostat.
 *
 * Thermostat is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your
 * option) any later version.
 *
 * Thermostat is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Thermostat; see the file COPYING.  If not see
 * <http://www.gnu.org/licenses/>.
 *
 * Linking this code with other modules is making a combined work
 * based on this code.  Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this code give
 * you permission to link this code with independent modules to
 * produce an executable, regardless of the license terms of these
 * independent modules, and to copy and distribute the resulting
 * executable under terms of your choice, provided that you also
 * meet, for each linked independent module, the terms and conditions
 * of the license of that module.  An independent module is a module
 * which is not derived from or based on this code.  If you modify
 * this code, you may extend this exception to your version of the
 * library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.redhat.thermostat.agent.command.internal;

import java.nio.charset.Charset;

interface CommandChannelConstants {

    // Server startup state tokens
    byte[] SERVER_STARTED_TOKEN = "<SERVER STARTED>".getBytes(Charset.forName("UTF-8"));
    byte[] SERVER_READY_TOKEN = "<SERVER READY>".getBytes(Charset.forName("UTF-8"));
    
    // SSLConfiguration JSON members
    String SSL_JSON_ROOT = "sslConfiguration";
    String SSL_JSON_KEYSTORE_FILE = "keystoreFile";
    String SSL_JSON_KEYSTORE_PASS = "keystorePass";
    String SSL_JSON_COMMAND_CHANNEL = "enabledCommandChannel";
    String SSL_JSON_BACKING_STORAGE = "enabledBackingStorage";
    String SSL_JSON_HOSTNAME_VERIFICATION = "disableHostnameVerification";
    
    // Request JSON members
    String REQUEST_JSON_TOP = "request";
    String REQUEST_JSON_TYPE = "type";
    String REQUEST_JSON_HOST = "targetHost";
    String REQUEST_JSON_PORT = "targetPort";
    String REQUEST_JSON_PARAMS = "parameters";
    
    // Response JSON members
    String RESPONSE_JSON_TOP = "response";
    String RESPONSE_JSON_TYPE = "type";

}
