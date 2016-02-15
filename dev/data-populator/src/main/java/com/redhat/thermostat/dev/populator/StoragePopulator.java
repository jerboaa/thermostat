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

package com.redhat.thermostat.dev.populator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.dev.populator.config.ConfigItem;
import com.redhat.thermostat.dev.populator.config.PopulationConfig;
import com.redhat.thermostat.dev.populator.dependencies.SharedState;
import com.redhat.thermostat.storage.core.Connection;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.Connection.ConnectionStatus;

class StoragePopulator {

    private final Arguments args;
    private final Map<String, CollectionPopulator> pojoPutters;
    
    StoragePopulator(Arguments args) {
        this.args = args;
        pojoPutters = getPopulatorsFromServiceLoader();
    }
    
    private static Map<String, CollectionPopulator> getPopulatorsFromServiceLoader() {
        ServiceLoader<CollectionPopulator> loader = ServiceLoader.load(CollectionPopulator.class);
        Map<String, CollectionPopulator> populators = new HashMap<>();
        for (CollectionPopulator putter: loader) {
            populators.put(putter.getHandledCollection(), putter);
        }
        return populators;
    }

    public void populate() {
        PopulationConfig config = getPopulationConfigFromArg();
        Storage storage = connectToStorage();
        addItemsToStorage(storage, config);
        disconnectStorage(storage);
        System.out.println("Data population finished successfully.");
    }

    private void disconnectStorage(Storage storage) {
        Connection conn = storage.getConnection();
        final CountDownLatch latch = new CountDownLatch(1);
        conn.addListener(new Connection.ConnectionListener() {
            
            @Override
            public void changed(ConnectionStatus newStatus) {
                switch (newStatus) {
                case DISCONNECTED:
                    latch.countDown();
                default:
                    break;
                }
                
            }
        });
        conn.disconnect();
        try {
            boolean expired = !latch.await(100, TimeUnit.MILLISECONDS);
            if (expired) {
                System.err.println("Timeout waiting for storage to disconnect.");
            }
        } catch (InterruptedException e) {
            // ignore
        }
    }

    private PopulationConfig getPopulationConfigFromArg() {
        File configFile = new File(args.getConfigFile());
        if (!configFile.exists()) {
            throw new IllegalArgumentException("Config file '" + configFile.getAbsolutePath() + "' does not exist!");
        }
        String json;
        PopulationConfig config;
        try {
            json = new String(Files.readAllBytes(configFile.toPath()));
            config = PopulationConfig.parseFromJsonString(json);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to parse config file", e);
        }
        return config;
    }

    /**
     * Add items to storage in topologically sorted order so as to have
     * relevant depending state available.
     * 
     * @param storage The storage to add records for
     * @param config The configuration describing the records to be added
     */
    private void addItemsToStorage(Storage storage, PopulationConfig config) {
        SharedState state = new SharedState();
        for (ConfigItem item: config.getConfigsTopologicallySorted()) {
            state = addRecordsForConfig(storage, item, state);
        }
    }

    private SharedState addRecordsForConfig(Storage storage, ConfigItem item, SharedState state) {
        CollectionPopulator putter = pojoPutters.get(item.getName());
        if (putter == null) {
            System.err.println("No populator for collection '" + item.getName() + "' found. Skipping.");
            return state; // return state unmodified
        } else {
            return putter.addPojos(storage, item, state);
        }
    }

    private Storage connectToStorage() {
        ConnectionEstablisher establisher = new ConnectionEstablisher(args);
        return establisher.connectToStorage();
    }
}
