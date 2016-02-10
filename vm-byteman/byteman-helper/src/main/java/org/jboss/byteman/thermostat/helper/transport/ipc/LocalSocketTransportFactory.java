/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package org.jboss.byteman.thermostat.helper.transport.ipc;

import static java.lang.System.getProperty;

import java.io.File;

import org.jboss.byteman.thermostat.Properties;
import org.jboss.byteman.thermostat.helper.Transport;

public class LocalSocketTransportFactory {
    
    private static final String SOCKET_NAME_PROPERTY = Properties.PREFIX + "socketName";
    private static final String SOCKET_CONFIG_PROPERTY = Properties.PREFIX + "ipcConfig";
    private static final String SOCKET_BATCH_SIZE_PROPERTY = Properties.PREFIX + "socketBatchSize";
    private static final String SOCKET_SEND_ATTEMPTS_PROPERTY = Properties.PREFIX + "socketSendAttempts";
    private static final String SOCKET_PAUSE_TIME_MILLIS_PROPERTY = Properties.PREFIX + "socketPauseTimeMillis";

    private static LocalSocketTransportFactory instance;
    
    public static Transport create() {
        int sendThreshold = Integer.parseInt(getProperty(Transport.SEND_THRESHOLD_PROPERTY, "0"));
        int loseThreshold = Integer.parseInt(getProperty(Transport.LOSE_THRESHOLD_PROPERTY, Integer.toString(Integer.MAX_VALUE)));
        String socketName = getProperty(SOCKET_NAME_PROPERTY, "byteman-thermostat-ipc");
        String ipcConfigStr = getProperty(SOCKET_CONFIG_PROPERTY, "i-do-not-exist");
        File ipcConfig = new File(ipcConfigStr);
        int socketBatchSize = Integer.parseInt(getProperty(SOCKET_BATCH_SIZE_PROPERTY, "8"));
        int socketAttempts = Integer.parseInt(getProperty(SOCKET_SEND_ATTEMPTS_PROPERTY, "1"));
        int socketBreak = Integer.parseInt(getProperty(SOCKET_PAUSE_TIME_MILLIS_PROPERTY, "100"));
        LocalSocketChannelFactory factory = new LocalSocketChannelFactoryImpl();
        CreatorHolder holder = new CreatorHolder(sendThreshold, loseThreshold, socketBatchSize, socketAttempts, socketBreak,
                          socketName, ipcConfig, factory);
        LocalSocketTransportFactory instance = LocalSocketTransportFactory.getInstance();
        return instance.create(holder);
    }
    
    private static synchronized LocalSocketTransportFactory getInstance() {
        if (instance == null) {
            instance = new LocalSocketTransportFactory();
        }
        return instance;
    }
    
    static synchronized void setInstance(LocalSocketTransportFactory factory) {
        instance = factory;
    }

    Transport create(CreatorHolder holder) {
        return holder.create();
    }
    
    static class CreatorHolder {
        
        private final int sendThreshold;
        private final int loseThreshold;
        private final int socketBatchSize;
        private final int socketAttempts;
        private final int socketBreak;
        private final String socketName;
        private final File ipcConfigFile;
        private final LocalSocketChannelFactory factory;
        
        CreatorHolder(int sendThreshold, int loseThreshold, int socketBatchSize, int socketAttempts, int socketBreak,
                      String socketName, File ipcConfigFile, LocalSocketChannelFactory factory) {
            this.sendThreshold = sendThreshold;
            this.loseThreshold = loseThreshold;
            this.socketBatchSize = socketBatchSize;
            this.socketAttempts = socketAttempts;
            this.socketBreak = socketBreak;
            this.socketName = socketName;
            this.ipcConfigFile = ipcConfigFile;
            this.factory = factory;
        }
        
        private Transport create() {
            return new LocalSocketTransport(sendThreshold, loseThreshold, ipcConfigFile, socketName, socketBatchSize,
                    socketAttempts, socketBreak, factory);
        }
        
        String getSocketName() {
            return socketName;
        }
        
        File getIpcConfigFile() {
            return ipcConfigFile;
        }
    }
}
