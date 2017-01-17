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

package com.redhat.thermostat.agent.ipc.common.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public abstract class IPCPropertiesBuilder {
    
    static final String PROP_IPC_TYPE = "type";
    
    private final PropertiesHelper helper;
    
    protected IPCPropertiesBuilder() {
        this(new PropertiesHelper());
    }
    
    IPCPropertiesBuilder(PropertiesHelper helper) {
        this.helper = helper;
    }
    
    public IPCProperties getProperties(File ipcProperties) throws IOException {
        Properties props = helper.createProperties();
        FileInputStream fis = null;
        try  {
            fis = helper.getInputStream(ipcProperties);
            props.load(fis);
            
            String typeString = props.getProperty(PROP_IPC_TYPE);
            if (typeString == null) {
                throw new IOException("No IPC type specified in property file");
            }
            IPCType type = IPCType.fromConfigValue(typeString);
            if (type == null) {
                throw new IOException("Unable to determine IPC type from property file ('"+typeString+"' not recognized)");
            }
            return getPropertiesForType(type, props, ipcProperties);
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }
    
    protected abstract IPCProperties getPropertiesForType(IPCType type, Properties props, File propFile) throws IOException;
    
    static class PropertiesHelper {
        
        Properties createProperties() {
            return new Properties();
        }
        
        FileInputStream getInputStream(File ipcProperties) throws IOException {
            return new FileInputStream(ipcProperties);
        }
        
    }

}
