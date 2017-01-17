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

package com.redhat.thermostat.vm.byteman.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.redhat.thermostat.web.common.typeadapters.PojoTypeAdapterFactory;
import com.redhat.thermostat.web.common.typeadapters.PreparedParameterTypeAdapterFactory;
import com.redhat.thermostat.web.common.typeadapters.PreparedParametersTypeAdapterFactory;
import com.redhat.thermostat.web.common.typeadapters.SharedStateIdTypeAdapterFactory;
import com.redhat.thermostat.web.common.typeadapters.WebPreparedStatementResponseTypeAdapterFactory;
import com.redhat.thermostat.web.common.typeadapters.WebPreparedStatementTypeAdapterFactory;
import com.redhat.thermostat.web.common.typeadapters.WebQueryResponseTypeAdapterFactory;

public class BytemanMetricTest {

    private Gson gson;
    
    @Before
    public void setup() {
        gson = new GsonBuilder()
                .registerTypeAdapterFactory(new PojoTypeAdapterFactory())
                .registerTypeAdapterFactory(new SharedStateIdTypeAdapterFactory())
                .registerTypeAdapterFactory(new WebPreparedStatementResponseTypeAdapterFactory())
                .registerTypeAdapterFactory(new WebQueryResponseTypeAdapterFactory())
                .registerTypeAdapterFactory(new PreparedParameterTypeAdapterFactory())
                .registerTypeAdapterFactory(new WebPreparedStatementTypeAdapterFactory())
                .registerTypeAdapterFactory(new PreparedParametersTypeAdapterFactory())
                .create();
    }
    
    @Test
    public void isTransformablefromPojo() {
        BytemanMetric metric = new BytemanMetric();
        metric.setAgentId("foo-agent");
        metric.setData("{ \"key\": \"value\" }");
        metric.setMarker("foo-marker");
        metric.setTimeStamp(1L);
        metric.setVmId("someVmId");
        String json = gson.toJson(metric, BytemanMetric.class);
        String expectedJson = "{\"agentId\":\"foo-agent\"," +
                "\"data\":\"{ \\\"key\\\": \\\"value\\\" }\"," + 
                "\"marker\":\"foo-marker\"," +
                "\"timeStamp\":1," +
                "\"vmId\":\"someVmId\"}";
        assertEquals(expectedJson, json);
    }
    
    @Test
    public void isTransformableToPojo() {
        String json = "{ \"agentId\":\"foo-agent\"," +
                        "\"data\": \"{ \\\"key\\\": \\\"value\\\" }\"," + 
                        "\"marker\": \"foo-marker\"," +
                        "\"timeStamp\":1," +
                        "\"vmId\":\"someVmId\"}";
        BytemanMetric metric = gson.fromJson(json, BytemanMetric.class);
        assertNotNull(metric);
        assertEquals("foo-agent", metric.getAgentId());
        assertEquals("foo-marker", metric.getMarker());
        assertEquals(1L, metric.getTimeStamp());
        String expectedData = "{ \"key\": \"value\" }";
        assertEquals(expectedData, metric.getData());
        assertEquals(expectedData, metric.getDataAsJson());
        assertEquals("someVmId", metric.getVmId());
    }
}
