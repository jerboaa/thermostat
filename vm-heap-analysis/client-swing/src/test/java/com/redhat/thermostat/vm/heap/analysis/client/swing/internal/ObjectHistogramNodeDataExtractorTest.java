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

package com.redhat.thermostat.vm.heap.analysis.client.swing.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.utils.DescriptorConverter;
import com.redhat.thermostat.vm.heap.analysis.common.HistogramRecord;
import com.redhat.thermostat.vm.heap.analysis.common.ObjectHistogram;

public class ObjectHistogramNodeDataExtractorTest {

    private static final String[] NODES = {"foo", "bar", "Jar"};
    private static final long TOTAL_SIZE = 1024;
    private static final int SOME_INT = 50;
    private static final double DELTA = 0.01;

    private ObjectHistogramNodeDataExtractor extractor;
    private HistogramRecord record;
    private ObjectHistogram histogram;

    @Before
    public void setUp() {
        extractor = new ObjectHistogramNodeDataExtractor();

        String className = NODES[0] + ObjectHistogramNodeDataExtractor.DELIMITER + NODES[1] +
                ObjectHistogramNodeDataExtractor.DELIMITER + NODES[2];
        record = new HistogramRecord(className, SOME_INT, TOTAL_SIZE);

        List<HistogramRecord> records = new ArrayList<>();
        records.add(record);
        records.add(new HistogramRecord("alpha.beta.Gamma", SOME_INT, SOME_INT));

        histogram = mock(ObjectHistogram.class);
        when(histogram.getHistogram()).thenReturn(records);
    }

    @Test
    public void testGetNodes() {
        String[] result = extractor.getNodes(record);
        assertTrue(Arrays.equals(NODES, result));

        String primitiveType = "Int";
        HistogramRecord primitiveRecord = new HistogramRecord(primitiveType, SOME_INT, SOME_INT);
        result = extractor.getNodes(primitiveRecord);
        assertEquals(1, result.length);
        assertEquals(DescriptorConverter.toJavaType(primitiveType), result[0]);
    }

    @Test
    public void testGetWeight() {
        double weight = extractor.getWeight(record);
        assertEquals(TOTAL_SIZE, weight, DELTA);
    }

    @Test
    public void testGetAsCollection() {
        Collection<HistogramRecord> collection = extractor.getAsCollection(histogram);
        assertEquals(2, collection.size());
        assertTrue(collection.contains(record));
    }
}
