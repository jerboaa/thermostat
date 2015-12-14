/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.vm.profiler.client.swing.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.utils.MethodDescriptorConverter.MethodDeclaration;
import com.redhat.thermostat.vm.profiler.client.core.ProfilingResult;
import com.redhat.thermostat.vm.profiler.client.core.ProfilingResult.MethodInfo;

public class ProfilingResultNodeDataExtractorTest {

    private static final String[] NODES = {"foo", "bar", "jar"};
    private static final long TOTAL_TIME = 1024;
    private static final int SOME_INT = 50;
    private static final double DELTA = 0.01;

    private ProfilingResultNodeDataExtractor extractor;
    private MethodInfo methodInfo;
    private ProfilingResult result;

    @Before
    public void setUp() {
        extractor = new ProfilingResultNodeDataExtractor();

        String methodName = NODES[0] + extractor.DELIMITER + NODES[1] + extractor.DELIMITER + NODES[2];
        MethodDeclaration decl = new MethodDeclaration(methodName, Arrays.asList("int"), "int");
        methodInfo = new MethodInfo(decl, TOTAL_TIME, SOME_INT);

        List<MethodInfo> methodInfos = new ArrayList<>();
        methodInfos.add(methodInfo);
        methodInfos.add(new MethodInfo(new MethodDeclaration("car", Arrays.asList("char"), "char"),
                SOME_INT, SOME_INT));
        result = new ProfilingResult(methodInfos);
    }

    @Test
    public void testGetNodes() {
        String[] result = extractor.getNodes(methodInfo);
        assertTrue(Arrays.equals(NODES, result));
    }

    @Test
    public void testGetWeight() {
        double weight = extractor.getWeight(methodInfo);
        assertEquals(TOTAL_TIME, weight, DELTA);
    }

    @Test
    public void testGetAsCollection() {
        Collection<MethodInfo> collection = extractor.getAsCollection(result);
        assertEquals(2, collection.size());
        assertTrue(collection.contains(methodInfo));
    }
}
