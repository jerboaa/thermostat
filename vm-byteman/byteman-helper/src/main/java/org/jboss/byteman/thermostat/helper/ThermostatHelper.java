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

package org.jboss.byteman.thermostat.helper;

import static org.jboss.byteman.thermostat.helper.Utils.toMap;

import java.util.LinkedHashMap;

import org.jboss.byteman.rule.Rule;
import org.jboss.byteman.rule.helper.Helper;

/**
 * Byteman helper that provides various {@code send()} methods for sending
 * metrics to a peer.
 * 
 */
public class ThermostatHelper extends Helper {
    
    // Lock to synchronize initialization of transport between instances
    private static final Object transportLock = new Object();
    private static Transport transport = null;

    /**
     * Constructor
     */
    protected ThermostatHelper(Rule rule) {
        super(rule);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (transportLock) {
                    if (transport != null) {
                        transport.close();
                    }
                }
            }
        }));
    }

    private static void initTransport() {
        synchronized (transportLock) {
            if (transport == null) {
                transport = new TransportFactory().create();
            }
        }
    }
    
    public void send(String marker, String key, String value) {
        send(marker, new Object[]{key, value});
    }
    
    public void send(String marker, String key, int value) {
        send(marker, new Object[]{key, value});
    }
    
    public void send(String marker, String key, long value) {
        send(marker, new Object[]{key, value});
    }
    
    public void send(String marker, String key, Number value) {
        send(marker, new Object[]{key, value});
    }
    
    public void send(String marker, String key, Boolean value) {
        send(marker, new Object[]{key, value});
    }

    public void send(String marker, String key1, String value1, String key2, String value2) {
        send(marker, new Object[]{key1, value1, key2, value2});
    }

    public void send(String marker, String key1, String value1, String key2, String value2,
                                  String key3, String value3) {
        send(marker, new Object[]{key1, value1, key2, value2, key3, value3});
    }

    public void send(String marker, Object... dataArray) {
        LinkedHashMap<String, Object> data = toMap(dataArray);
        BytemanMetric rec = new BytemanMetric(marker, data);
        synchronized (transportLock) {
            transport.send(rec);
        }
    }
    
    static void setTransport(Transport transport) {
        synchronized (transportLock) {
            ThermostatHelper.transport = transport;
        }
    }
    
    // package-private for testing
    static Transport getTransport() {
        return transport;
    }
    
    // Called by Byteman on helper de-activation
    public static void deactivated() {
        synchronized (transportLock) {
            transport.close();
        }
        setTransport(null);
    }
    
    // Called by Byteman on helper activation
    public static void activated() {
        initTransport();
    }

}
