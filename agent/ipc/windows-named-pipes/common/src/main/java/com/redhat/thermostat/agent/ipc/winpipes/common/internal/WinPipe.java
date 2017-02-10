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

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * WinPipe performs basic operations (mostly serverside) on a Windows named pipe.
 */
public class WinPipe {

    private final WinPipesNativeHelper helper;

    public static class Id {
        private final String id;
        Id(final String id) {
            this.id = id;
        }
        String getPipePath() {
            return id;
        }
        public String toString() {
            return "pipeid:" + id;
        }
    }

    private final Id pipeid;
    private long handle;

    public WinPipe(String pipeName) {
        this.pipeid = new Id(pipeName);
        this.helper = WinPipesNativeHelper.INSTANCE;
        this.handle = WinPipesNativeHelper.INVALID_HANDLE;
    }

    WinPipe(Id id) {
        this.pipeid = id;
        this.helper = WinPipesNativeHelper.INSTANCE;
        this.handle = WinPipesNativeHelper.INVALID_HANDLE;
    }

    // for testing
    public WinPipe(WinPipesNativeHelper helper, String pipeName) {
        this.pipeid = new Id(pipeName);
        this.helper = helper;
        this.handle = WinPipesNativeHelper.INVALID_HANDLE;
    }

    public WinPipe(WinPipesNativeHelper helper, Id id) {
        this.pipeid = id;
        this.helper = helper;
        this.handle = WinPipesNativeHelper.INVALID_HANDLE;
    }

    long open() throws IOException {
        handle = helper.openNamedPipe(pipeid.getPipePath());
        if (!isOpen()) {
            throw new IOException("Can't open pipe " + getPipeName() + " err=" + helper.getLastError());
        }
        return isOpen() ? handle : WinPipesNativeHelper.INVALID_HANDLE;
    }

    public long createWindowsNamedPipe(int numInstances, int bufsize) {
        handle = helper.createNamedPipe(pipeid.getPipePath(), numInstances, bufsize);
        return isOpen() ? handle : WinPipesNativeHelper.INVALID_HANDLE;
    }

    public void close() {
        helper.closeHandle(handle);
        handle = WinPipesNativeHelper.INVALID_HANDLE;
    }

    public boolean isOpen() {
        return handle != WinPipesNativeHelper.INVALID_HANDLE;
    }

    public String getPipeName() {
        return pipeid.getPipePath();
    }

    public String toString() {
        return "winpipe:" + pipeid;
    }

    int write(ByteBuffer buffer) {
        return helper.writeFile(handle, buffer);
    }

    int read(ByteBuffer buffer) {
        return helper.readFile(handle, buffer);
    }
}
