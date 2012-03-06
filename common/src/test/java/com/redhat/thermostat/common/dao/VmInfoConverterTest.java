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

package com.redhat.thermostat.common.dao;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.redhat.thermostat.common.VmInfo;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Key;

public class VmInfoConverterTest {

    @Test
    public void testVmInfoToChunk() {
        Map<String, String> environment = new HashMap<String, String>();
        Map<String, String> properties = new HashMap<String, String>();
        List<String> loadedNativeLibraries = new ArrayList<String>();
        VmInfo info = new VmInfo(0, 1, 2,
                "java-version", "java-home", "main-class", "command-line",
                "vm-name", "vm-info", "vm-version", "vm-arguments",
                properties, environment, loadedNativeLibraries);

        Chunk chunk = new VmInfoConverter().vmInfoToChunk(info);

        assertNotNull(chunk);
        assertEquals((Integer) 0, chunk.get(new Key<Integer>("vm-id", true)));
        assertEquals((Integer) 0, chunk.get(new Key<Integer>("vm-pid", false)));
        assertEquals((Long) 1l, chunk.get(new Key<Long>("start-time", false)));
        assertEquals((Long) 2l, chunk.get(new Key<Long>("stop-time", false)));
        assertEquals("java-version", chunk.get(new Key<String>("runtime-version", false)));
        assertEquals("java-home", chunk.get(new Key<String>("java-home", false)));
        assertEquals("main-class", chunk.get(new Key<String>("main-class", false)));
        assertEquals("command-line", chunk.get(new Key<String>("command-line", false)));
        assertEquals("vm-name", chunk.get(new Key<String>("vm-name", false)));
        assertEquals("vm-info", chunk.get(new Key<String>("vm-info", false)));
        assertEquals("vm-version", chunk.get(new Key<String>("vm-version", false)));
        assertEquals("vm-arguments", chunk.get(new Key<String>("vm-arguments", false)));

        // FIXME test environment, properties and loaded native libraries later
    }
}
