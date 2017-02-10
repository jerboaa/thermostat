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

package com.redhat.thermostat.agent.ipc.winpipes.common.internal;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import com.redhat.thermostat.agent.ipc.common.internal.IPCProperties;
import com.redhat.thermostat.agent.ipc.common.internal.IPCType;

public class WinPipesIPCProperties extends IPCProperties {

    static final String GLOBAL_WINPIPE_PREFIX = "\\\\.\\pipe\\";
    static final String DEFAULT_WINPIPE_PREFIX = "thermostat-pipe-";

    static final String PIPE_PREFIX_PROPERTY = "winpipe.prefix";
    static final String WINPIPE_ID_PROPERTY = "winpipe.id";

    private final String pipePrefix;
    private final String pipeId;

    WinPipesIPCProperties(Properties props, File propFile) throws IOException {
        this(props, propFile, new PathUtils());
    }

    WinPipesIPCProperties(final Properties props, final File propFile, final PathUtils pathUtils) throws IOException {
        super(IPCType.WINDOWS_NAMED_PIPES, propFile);
        this.pipePrefix = pathUtils.getProperty(props, PIPE_PREFIX_PROPERTY, DEFAULT_WINPIPE_PREFIX);
        this.pipeId = pathUtils.getProperty(props, WINPIPE_ID_PROPERTY, "");
    }

    public String getPipeName(final String serverName) {
        final String pipeid = getPipeId().isEmpty() ? "" : getPipeId() + '-';
        return GLOBAL_WINPIPE_PREFIX + pipePrefix + pipeid + serverName;
    }

    String getPipePrefix() {
        return pipePrefix;
    }

    private String getPipeId() {
        return pipeId;
    }

    // Helper class for testing purposes
    static class PathUtils {
        String getProperty(final Properties props, final String name, final String dflt) {
            return props.getProperty(name, dflt);
        }
    }
}
