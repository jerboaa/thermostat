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

package com.redhat.thermostat.backend.sample;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.backend.Backend;
import com.redhat.thermostat.common.storage.Category;
import com.redhat.thermostat.common.utils.LoggingUtils;

/** Just an example backend implementation.  This is really just to test the loading and configuration mechanisms
 *
 */
public class SampleBackend extends Backend {
    private final String NAME = "sample-backend";
    private final String DESCRIPTION = "A backend which does nothing at all.";
    private final String VENDOR = "Nobody";
    private final String VERSION = "0.1";
    private boolean currentlyActive = false;

    private Logger logger = LoggingUtils.getLogger(SampleBackend.class);

    public SampleBackend() {
        super();
    }

    @Override
    protected void setConfigurationValue(String name, String value) {
        logger.log(Level.FINE, "Setting configuration value for backend: " + this.NAME);
        logger.log(Level.FINE, "key: " + name + "    value: " + value);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String getVendor() {
        return VENDOR;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public Map<String, String> getConfigurationMap() {
        return new HashMap<String, String>();
    }

    @Override
    public String getConfigurationValue(String key) {
        throw new IllegalArgumentException("SampleBackend does not actually accept any configuration.");
    }

    @Override
    public boolean activate() {
        currentlyActive = true;
        return true;
    }

    @Override
    public boolean deactivate() {
        currentlyActive = false;
        return true;
    }

    @Override
    public boolean isActive() {
        return currentlyActive;
    }

    @Override
    protected Iterator<Category> getCategoryIterator() {
        return new HashSet<Category>().iterator();
    }

    @Override
    public boolean attachToNewProcessByDefault() {
        return false;
    }

}
