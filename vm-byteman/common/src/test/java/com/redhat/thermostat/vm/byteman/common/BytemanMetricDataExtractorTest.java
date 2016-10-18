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

package com.redhat.thermostat.vm.byteman.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class BytemanMetricDataExtractorTest {

    private BytemanMetricDataExtractor extractor;
    
    @Before
    public void setup() {
        extractor = new BytemanMetricDataExtractor();
    }
    
    @Test
    public void testEmptyKeySet() {
        List<String> actual = extractor.getSortedKeySet();
        assertTrue(actual.isEmpty());
    }
    
    @Test
    public void testAdditiveKeySet() {
        String firstKey = "zoo";
        BytemanMetric first = new BytemanMetric();
        first.setData("{\"" + firstKey + "\": \"bar\"}");
        extractor.mineMetric(first);
        List<String> actual = extractor.getSortedKeySet();
        assertEquals(1, actual.size());
        assertEquals(firstKey, actual.get(0));
        
        BytemanMetric second = new BytemanMetric();
        String secondKey = "secondKey";
        second.setData("{\"" + secondKey + "\": \"baz\"}");
        extractor.mineMetric(second);
        actual = extractor.getSortedKeySet();
        assertEquals(2, actual.size());
        
        // verify sorting
        assertEquals(secondKey, actual.get(0));
        assertEquals(firstKey, actual.get(1));
    }
    
    @Test
    public void testDuplicateKey() {
        String firstKey = "zoo";
        BytemanMetric first = new BytemanMetric();
        first.setData("{\"" + firstKey + "\": \"bar\"}");
        extractor.mineMetric(first);
        List<String> actual = extractor.getSortedKeySet();
        assertEquals(1, actual.size());
        assertEquals(firstKey, actual.get(0));
        
        extractor.mineMetric(first);
        actual = extractor.getSortedKeySet();
        assertEquals(1, actual.size());
        assertEquals(firstKey, actual.get(0));
    }
    
    @Test
    public void extractionDoesNotFailIfNoData() {
        BytemanMetric m = new BytemanMetric();
        extractor.mineMetric(m); // this shall not fail with NPE
        List<String> actual = extractor.getSortedKeySet();
        assertTrue(actual.isEmpty());
    }
    
    @Test(expected = NullPointerException.class)
    public void mineRequiresNonNull() {
        extractor.mineMetric(null);
    }
    
    @Test
    public void canExtractListOfMetrics() {
        String[] keys = new String[] {
                "001_first", "002_second", "003_third", "004_fourth"
        };
        List<BytemanMetric> mList = buildMetrics(keys);
        for (BytemanMetric metric: mList) {
            extractor.mineMetric(metric);
        }
        List<String> keySet = extractor.getSortedKeySet();
        assertEquals(4, keySet.size());
        for (int i = 0; i < keySet.size(); i++) {
            assertEquals(keys[0], keySet.get(0));
        }
        
    }

    private List<BytemanMetric> buildMetrics(String[] keys) {
        List<BytemanMetric> list = new ArrayList<>();
        for (String key: keys) {
            BytemanMetric m = new BytemanMetric();
            m.setData("{\"" + key + "\": \"" + key + "_value\"}");
            list.add(m);
        }
        return list;
    }
}
