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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.redhat.thermostat.shared.config.SSLConfiguration;

class SSLConfigurationEncoder {
    
    // Total size of JSON encoded SSLConfiguration should be no more than this size in bytes
    private static final int SSL_CONF_MAX_BYTES = 8192;
    
    byte[] encodeAsJson(SSLConfiguration sslConf) throws IOException {
        GsonBuilder builder = new GsonBuilder();
        builder.serializeNulls(); // Necessary since keystore file/password can be null
        Gson gson = builder.create();
        
        JsonObject paramsObj = new JsonObject();
        File keystoreFile = sslConf.getKeystoreFile();
        String keystorePath = null;
        if (keystoreFile != null) {
            keystorePath = keystoreFile.getAbsolutePath();
        }
        paramsObj.addProperty(CommandChannelConstants.SSL_JSON_KEYSTORE_FILE, keystorePath);
        paramsObj.addProperty(CommandChannelConstants.SSL_JSON_KEYSTORE_PASS, sslConf.getKeyStorePassword());
        paramsObj.addProperty(CommandChannelConstants.SSL_JSON_COMMAND_CHANNEL, sslConf.enableForCmdChannel());
        paramsObj.addProperty(CommandChannelConstants.SSL_JSON_BACKING_STORAGE, sslConf.enableForBackingStorage());
        paramsObj.addProperty(CommandChannelConstants.SSL_JSON_HOSTNAME_VERIFICATION, sslConf.disableHostnameVerification());
        
        JsonObject sslConfigRoot = new JsonObject();
        sslConfigRoot.add(CommandChannelConstants.SSL_JSON_ROOT, paramsObj);
        
        String jsonSslConf = gson.toJson(sslConfigRoot);
        byte[] jsonSslConfBytes = jsonSslConf.getBytes(Charset.forName("UTF-8"));
        if (jsonSslConfBytes.length > SSL_CONF_MAX_BYTES) {
            throw new IOException("JSON-encoded SSL configuration larger than maximum of "
                    + SSL_CONF_MAX_BYTES + " bytes");
        }
        return jsonSslConfBytes;
    }
    
    

}
