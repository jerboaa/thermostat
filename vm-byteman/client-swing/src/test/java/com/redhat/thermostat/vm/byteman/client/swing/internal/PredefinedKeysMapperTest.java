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

package com.redhat.thermostat.vm.byteman.client.swing.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.vm.byteman.client.swing.internal.PredefinedKeysMapper.MapDirection;

public class PredefinedKeysMapperTest {

    private PredefinedKeysMapper mapper;
    
    @Before
    public void setup() {
        mapper = new PredefinedKeysMapper();
    }
    
    @Test
    public void testMapKeysNull() {
        String mapped = mapper.mapPredefinedKey(null, MapDirection.MODEL_TO_VIEW);
        assertNull(mapped);
        mapped = mapper.mapPredefinedKey(null, MapDirection.VIEW_TO_MODEL);
        assertNull(mapped);
    }
    
    @Test
    public void testMapNoFilterNull() {
        String mapped = mapper.mapNoFilter(null, MapDirection.MODEL_TO_VIEW);
        assertNull(mapped);
        mapped = mapper.mapNoFilter(null, MapDirection.VIEW_TO_MODEL);
        assertNull(mapped);
    }
    
    /**
     * Only empty string and localized {@code No Filter} get mapped.
     */
    @Test
    public void testMapNoFilterUntracked() {
        String filterValUntracked = "foobar";
        assertFalse("Precondition not met", PredefinedKeysMapper.NO_FILTER_ITEM.equals(filterValUntracked));
        String mapped = mapper.mapNoFilter(filterValUntracked, MapDirection.MODEL_TO_VIEW);
        assertSame(filterValUntracked, mapped);
        mapped = mapper.mapNoFilter(filterValUntracked, MapDirection.VIEW_TO_MODEL);
        assertSame(filterValUntracked, mapped);
    }
    
    /**
     * Only predefined keys shall map. Others should get returned verbatim.
     */
    @Test
    public void testMapPredefinedKeysUntracked() {
        String keyValUntracked = "foobar";
        String mapped = mapper.mapPredefinedKey(keyValUntracked, MapDirection.MODEL_TO_VIEW);
        assertSame(keyValUntracked, mapped);
        mapped = mapper.mapPredefinedKey(keyValUntracked, MapDirection.VIEW_TO_MODEL);
        assertSame(keyValUntracked, mapped);
    }
    
    @Test
    public void testMapFromModelPredefinedKeys() {
        for (String keyModel: PredefinedKeysMapper.PREDEFINED_KEYS) {
            String mapped = mapper.mapPredefinedKey(keyModel, MapDirection.MODEL_TO_VIEW);
            String expected = PredefinedKeysMapper.PREDEFINED_PREFIX + keyModel + PredefinedKeysMapper.PREDEFINED_SUFFIX;
            assertEquals(expected, mapped);
        }
    }
    
    @Test
    public void testMapFromViewPredefinedKeys() {
        for (String keyModel: PredefinedKeysMapper.PREDEFINED_KEYS) {
            String preMapping = PredefinedKeysMapper.PREDEFINED_PREFIX + keyModel + PredefinedKeysMapper.PREDEFINED_SUFFIX;
            String mapped = mapper.mapPredefinedKey(preMapping, MapDirection.VIEW_TO_MODEL);
            assertEquals(keyModel, mapped);
        }
    }
    
    @Test
    public void testMapFromModelNoFilter() {
        String noFilterModel = "";
        String mapped = mapper.mapNoFilter(noFilterModel, MapDirection.MODEL_TO_VIEW);
        assertEquals(PredefinedKeysMapper.NO_FILTER_ITEM, mapped);
    }
    
    @Test
    public void testMapFromViewNoFilter() {
        String noFilterView = PredefinedKeysMapper.NO_FILTER_ITEM;
        String mapped = mapper.mapNoFilter(noFilterView, MapDirection.VIEW_TO_MODEL);
        String noFilterModelExpected = "";
        assertEquals(noFilterModelExpected, mapped);
    }
}
