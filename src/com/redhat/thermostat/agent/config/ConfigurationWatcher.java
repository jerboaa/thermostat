/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.agent.config;

import java.util.logging.Logger;

import com.redhat.thermostat.agent.storage.Storage;
import com.redhat.thermostat.backend.BackendRegistry;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class ConfigurationWatcher implements Runnable {

    private static final Logger logger = LoggingUtils.getLogger(ConfigurationWatcher.class);

    private Storage storage;
    private BackendRegistry backends;

    public ConfigurationWatcher(Storage storage, BackendRegistry backends) {
        this.storage = storage;
        this.backends = backends;
    }

    @Override
    public void run() {
        logger.fine("Watching for configuration changes.");
        while (!Thread.interrupted()) {
            checkConfigUpdates();
        }
        logger.fine("No longer watching for configuration changes.");
    }

    // TODO It would be best to develop this algorithm when we have a client that can initiate changes, so that it can be tested.
    private void checkConfigUpdates() {
        try { // THIS IS ONLY TEMPORARY.  Until we do implement this algorithm, we don't want this thread busy hogging CPU.
            Thread.sleep(2000);
        } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
        }
    }

}
