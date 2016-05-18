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

package org.jboss.byteman.thermostat.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.concurrent.CountDownLatch;

import org.jboss.byteman.rule.Rule;
import org.jboss.byteman.thermostat.helper.transport.stdout.TestStdOutTransport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class ThermostatHelperTest {

    private static final double DELTA = 0.001;
    private ThermostatHelper helper;
    private Transport mockTransport;
    
    @Before
    public void setup() {
        mockTransport = mock(Transport.class);
        ThermostatHelper.setTransport(mockTransport);
        helper = new ThermostatHelper(mock(Rule.class));
    }
    
    @After
    public void teardown() {
        ThermostatHelper.setTransport(null);
    }
    
    @Test
    public void canSendSimpleValue() {
        helper.send("my-marker", "key1", "value1");
        ArgumentCaptor<BytemanMetric> metricCaptor = ArgumentCaptor.forClass(BytemanMetric.class);
        verify(mockTransport).send(metricCaptor.capture());
        BytemanMetric metric = metricCaptor.getValue();
        assertNotNull(metric);
        assertEquals("my-marker", metric.getMarker());
        LinkedHashMap<String, Object> data = metric.getData();
        assertTrue(data.containsKey("key1"));
        assertEquals("value1", data.get("key1"));
    }
    
    @Test
    public void canSendSimpleValueMultiple2() {
        helper.send("my-marker", "key1", "value1", "key2", 3_000);
        ArgumentCaptor<BytemanMetric> metricCaptor = ArgumentCaptor.forClass(BytemanMetric.class);
        verify(mockTransport).send(metricCaptor.capture());
        BytemanMetric metric = metricCaptor.getValue();
        assertNotNull(metric);
        assertEquals("my-marker", metric.getMarker());
        LinkedHashMap<String, Object> data = metric.getData();
        assertTrue(data.containsKey("key1"));
        assertEquals("value1", data.get("key1"));
        assertTrue(data.containsKey("key2"));
        assertEquals(3000, data.get("key2"));
    }
    
    @Test
    public void canSendSimpleValueMultiple3() {
        helper.send("my-marker", "key1", "value1", "key2", 3_000, "key3", Math.PI);
        ArgumentCaptor<BytemanMetric> metricCaptor = ArgumentCaptor.forClass(BytemanMetric.class);
        verify(mockTransport).send(metricCaptor.capture());
        BytemanMetric metric = metricCaptor.getValue();
        assertNotNull(metric);
        assertEquals("my-marker", metric.getMarker());
        LinkedHashMap<String, Object> data = metric.getData();
        assertTrue(data.containsKey("key1"));
        assertEquals("value1", data.get("key1"));
        assertTrue(data.containsKey("key2"));
        assertEquals(3000, data.get("key2"));
        assertTrue(data.containsKey("key3"));
        assertEquals(Math.PI, (double)data.get("key3"), DELTA);
    }

    @Test
    public void canSendMapMultiple() {
        Object[] keyValuePairs = new Object[100];
        boolean flipMe = false;
        for (int i = 0; i < keyValuePairs.length; i++) {
            if (i % 2 == 0) {
                keyValuePairs[i] = "key" + i;
            } else {
                flipMe = !flipMe;
                keyValuePairs[i] = getValue(flipMe, i);
            }
        }
        helper.send("test-marker", keyValuePairs);
        ArgumentCaptor<BytemanMetric> metricCaptor = ArgumentCaptor.forClass(BytemanMetric.class);
        verify(mockTransport).send(metricCaptor.capture());
        BytemanMetric metric = metricCaptor.getValue();
        assertNotNull(metric);
        assertEquals("test-marker", metric.getMarker());
        LinkedHashMap<String, Object> data = metric.getData();
        assertEquals(50, data.keySet().size());
        assertEquals(-99, data.get("key98"));
    }
    
    @Test
    public void verifyMultipleCallsToSend() {
        String marker1 = "foobar";
        helper.send(marker1, "key1", "value2");
        String marker2 = "spicy";
        helper.send(marker2, "testKey", 5000);
        ArgumentCaptor<BytemanMetric> metricCaptor = ArgumentCaptor.forClass(BytemanMetric.class);
        verify(mockTransport, times(2)).send(metricCaptor.capture());
        BytemanMetric first = metricCaptor.getAllValues().get(0);
        BytemanMetric second = metricCaptor.getAllValues().get(1);
        assertEquals(marker2, second.getMarker());
        assertEquals(marker1, first.getMarker());
        assertEquals("value2", first.getData().get("key1"));
        assertEquals(5000, second.getData().get("testKey"));
    }
    
    @Test
    public void testBasicWithStdoutTransport() throws InterruptedException {
        final PrintStream oldOut = System.out;
        final CountDownLatch latch = new CountDownLatch(1);
        Transport stdoutTransport = new TestStdOutTransport(latch);
        ThermostatHelper.setTransport(stdoutTransport);
        ThermostatHelper helperWithStdout = new ThermostatHelper(mock(Rule.class));
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        PrintStream testSysout = new PrintStream(bout);
        System.setOut(testSysout);
        try {
            helperWithStdout.send("test-boolean-marker", "mykey", true);
            latch.await(); // wait for messages to arrive (happens asynchronously)
            String sentStuff = new String(bout.toByteArray());
            assertTrue(sentStuff.contains("test-boolean-marker"));
            assertTrue(sentStuff.contains("true"));
            assertTrue(sentStuff.contains("mykey"));
        } finally {
            System.setOut(oldOut);
        }
    }

    private Object getValue(boolean stringValue, int i) {
        if (stringValue) {
            return "value" + i;
        } else {
            return -i;
        }
    }
}