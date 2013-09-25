/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.web.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.redhat.thermostat.storage.core.PreparedParameter;
import com.redhat.thermostat.storage.core.auth.DescriptorMetadata;
import com.redhat.thermostat.storage.core.auth.StatementDescriptorRegistration;

public class KnownDescriptorRegistryTest {

    @Test
    public void canAddDescriptors() {
        Set<String> descs = new HashSet<>();
        descs.add("QUERY test WHERE 'a' = ?s");
        descs.add("QUERY agent-config");
        Iterable<StatementDescriptorRegistration> regs = getRegs(descs);
        KnownDescriptorRegistry reg = new KnownDescriptorRegistry(regs);
        Set<String> registeredDescs = null;
        try {
            registeredDescs = reg.getRegisteredDescriptors();
        } catch (IllegalStateException e) {
            fail(e.getMessage());
        }
        assertNotNull(registeredDescs);
        for (String d: registeredDescs) {
            assertTrue(descs.contains(d));
        }
    }
    
    @Test
    public void testServiceLoadersInClassPath() {
        KnownDescriptorRegistry reg = new KnownDescriptorRegistry();
        Set<String> trustedDescs = reg.getRegisteredDescriptors();
        assertNotNull(trustedDescs);
        // storage-core registers 22 descriptors; this module has
        // only storage-core as maven dep which registers queries.
        // see DAOImplStatementDescriptorRegistration
        assertEquals(22, trustedDescs.size());
    }
    
    @Test
    public void cannotAddNullDescriptors() {
        Set<String> descs = new HashSet<>();
        descs.add("QUERY test WHERE 'a' = ?s");
        descs.add(null);
        Iterable<StatementDescriptorRegistration> regs = getRegs(descs);
        try {
            new KnownDescriptorRegistry(regs);
            fail("Should not have accepted null descriptor");
        } catch (IllegalStateException e) {
            assertEquals("null statement descriptor not acceptable!", e.getMessage());
        }
    }

    private Iterable<StatementDescriptorRegistration> getRegs(Set<String> descs) {
        StatementDescriptorRegistration reg = new TestStatementDescriptorRegistration(
                descs, null);
        StatementDescriptorRegistration[] regs = new StatementDescriptorRegistration[] { reg };
        return Arrays.asList(regs);
    }
    
    private static class TestStatementDescriptorRegistration implements StatementDescriptorRegistration {

        private final Set<String> descs;
        private final Map<String, DescriptorMetadata> metadata;
        
        private TestStatementDescriptorRegistration(Set<String> descs, Map<String, DescriptorMetadata> metadata) {
            this.descs = descs;
            this.metadata = metadata;
        }
        
        @Override
        public Set<String> getStatementDescriptors() {
            return descs;
        }

        @Override
        public DescriptorMetadata getDescriptorMetadata(String descriptor,
                PreparedParameter[] params) {
            return metadata.get(descriptor);
        }
        
    }
}
