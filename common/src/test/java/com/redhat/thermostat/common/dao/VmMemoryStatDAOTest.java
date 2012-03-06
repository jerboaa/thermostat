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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.Test;

import com.redhat.thermostat.common.storage.Key;

public class VmMemoryStatDAOTest {
    @Test
    public void testCategories() {
        Collection<Key<?>> keys;

        assertEquals("vm-memory-stats", VmMemoryStatDAO.vmMemoryStatsCategory.getName());
        keys = VmMemoryStatDAO.vmMemoryStatsCategory.getKeys();
        assertTrue(keys.contains(new Key<Integer>("vm-id", false)));
        assertTrue(keys.contains(new Key<Long>("timestamp", false)));
        assertTrue(keys.contains(new Key<String>("eden.gen", false)));
        assertTrue(keys.contains(new Key<String>("eden.collector", false)));
        assertTrue(keys.contains(new Key<Long>("eden.capacity", false)));
        assertTrue(keys.contains(new Key<Long>("eden.max-capacity", false)));
        assertTrue(keys.contains(new Key<Long>("eden.used", false)));
        assertTrue(keys.contains(new Key<String>("s0.gen", false)));
        assertTrue(keys.contains(new Key<String>("s0.collector", false)));
        assertTrue(keys.contains(new Key<Long>("s0.capacity", false)));
        assertTrue(keys.contains(new Key<Long>("s0.max-capacity", false)));
        assertTrue(keys.contains(new Key<Long>("s0.used", false)));
        assertTrue(keys.contains(new Key<String>("s1.gen", false)));
        assertTrue(keys.contains(new Key<String>("s1.collector", false)));
        assertTrue(keys.contains(new Key<Long>("s1.capacity", false)));
        assertTrue(keys.contains(new Key<Long>("s1.max-capacity", false)));
        assertTrue(keys.contains(new Key<Long>("s1.used", false)));
        assertTrue(keys.contains(new Key<String>("old.gen", false)));
        assertTrue(keys.contains(new Key<String>("old.collector", false)));
        assertTrue(keys.contains(new Key<Long>("old.capacity", false)));
        assertTrue(keys.contains(new Key<Long>("old.max-capacity", false)));
        assertTrue(keys.contains(new Key<Long>("old.used", false)));
        assertTrue(keys.contains(new Key<String>("perm.gen", false)));
        assertTrue(keys.contains(new Key<String>("perm.collector", false)));
        assertTrue(keys.contains(new Key<Long>("perm.capacity", false)));
        assertTrue(keys.contains(new Key<Long>("perm.max-capacity", false)));
        assertTrue(keys.contains(new Key<Long>("perm.used", false)));
        assertEquals(27, keys.size());
    }
}
