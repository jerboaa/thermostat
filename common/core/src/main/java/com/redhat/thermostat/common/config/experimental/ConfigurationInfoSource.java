/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.common.config.experimental;

import java.io.IOException;
import java.util.Map;

import com.redhat.thermostat.annotations.Service;

/**
 * Provides plugins with access to their config files through the use of their plugin ID and config filenames
 * defined in their thermostat-plugin.xml file. Plugins may request this service through their Activator's
 * BundleContext. If no {@code configurations} tag is specified, config files by default are searched for in
 * $THERMOSTAT_HOME/etc/plugins.d/$PLUGIN_ID/ and/or $USER_THERMOSTAT_HOME/etc/plugins.d/$PLUGIN_ID/.
 * Users may provide custom locations through the {@code configurations} tag in their thermostat-plugin.xml.
 * The tag expects absolute paths
 *
 * Note: The configuration files here are visible to other plugins and is not appropriate for storing
 * confidential information.
 */

@Service
public interface ConfigurationInfoSource {
    public Map<String, String> getConfiguration(String pluginID, String fileName) throws IOException;
}
