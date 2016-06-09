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

package com.redhat.thermostat.vm.byteman.agent.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.vm.byteman.agent.internal.BytemanAttacher.BtmInstallHelper;

public class BytemanAttacherTest {
    
    private static final String DEFAULT_PROPERTY_VALUE = "<unset>";
    private static final String BYTEMAN_VERBOSE_PROPERTY = "org.jboss.byteman.verbose";
    private static final String THERMOSTAT_HELPER_SOCKET_NAME_PROPERTY = "org.jboss.byteman.thermostat.socketName";
    private static final String THERMOSTAT_IPC_CONFIG_PROPERTY = "org.jboss.byteman.thermostat.ipcConfig";
    private static final String BYTEMAN_PREFIX = "org.jboss.byteman.";

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java BytemanAttacherTest <PID_OF_JVM>");
            System.exit(1);
        }
        BytemanAttacher attacher = new BytemanAttacher(mock(CommonPaths.class));
        BytemanAgentInfo info = attacher.attach("barVmId", Integer.parseInt(args[0]), "fooAgent");
        if (info != null) {
            System.out.println("Byteman agent attached successfully");
            System.out.println("Byteman agent listening on port "+ info.getAgentListenPort());
        } else {
            System.out.println("Byteman agent attaching FAILED");
        }
    }
    
    @Test
    public void attachSetsAppropriateProperties() throws Exception {
        String filePath = "/path/to/run/data";
        CommonPaths paths = mock(CommonPaths.class);
        File mockFile = mock(File.class);
        when(mockFile.getAbsolutePath()).thenReturn(filePath);
        when(paths.getUserIPCConfigurationFile()).thenReturn(mockFile);
        BtmInstallHelper installer = mock(BtmInstallHelper.class);
        ArgumentCaptor<String[]> propsCaptor = ArgumentCaptor.forClass(String[].class);
        BytemanAttacher attacher = new BytemanAttacher(installer, paths);
        attacher.attach("testVmId", 9999, "fooAgent");
        verify(installer).install(eq(Integer.toString(9999)), eq(true), eq(false), eq((String)null), any(int.class), propsCaptor.capture());
        Map<String, String> properties = buildMapFromStringProps(propsCaptor.getValue());
        verifyPropertiesStartWithBytemanPrefix(properties);
        assertTrue(properties.containsKey(BYTEMAN_VERBOSE_PROPERTY));
        String socketName = properties.get(THERMOSTAT_HELPER_SOCKET_NAME_PROPERTY);
        String expectedName = new VmSocketIdentifier("testVmId", 9999, "fooAgent").getName();
        assertEquals(expectedName, socketName);
        String ipcConfig = properties.get(THERMOSTAT_IPC_CONFIG_PROPERTY);
        assertEquals(filePath, ipcConfig);
    }

    // Setting properties via the byteman agent is only allowed if the propery
    // name starts with org.jboss.byteman. Be sure that all our props we want to
    // set actually start with that prefix.
    private void verifyPropertiesStartWithBytemanPrefix(Map<String, String> properties) {
        for (String prop: properties.keySet()) {
            assertTrue("Expected property to start with " + BYTEMAN_PREFIX + " " +
                       "but was: " + prop, prop.startsWith(BYTEMAN_PREFIX));
        }
    }

    private Map<String, String> buildMapFromStringProps(String[] values) {
        Map<String, String> properties = new HashMap<>();
        for (String item: values) {
            String[] pair = item.split("=");
            if (pair.length == 1) {
                properties.put(pair[0], DEFAULT_PROPERTY_VALUE);
            } else if (pair.length == 2) {
                properties.put(pair[0], pair[1]);
            } else {
                throw new AssertionError("Illegal property item: " + item);
            }
        }
        return properties;
    }
}
