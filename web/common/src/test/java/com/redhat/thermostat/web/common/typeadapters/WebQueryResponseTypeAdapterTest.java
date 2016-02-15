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

package com.redhat.thermostat.web.common.typeadapters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Type;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.HostInfo;
import com.redhat.thermostat.web.common.PreparedStatementResponseCode;
import com.redhat.thermostat.web.common.WebQueryResponse;
import com.redhat.thermostat.web.common.typeadapters.PojoTypeAdapterFactory;
import com.redhat.thermostat.web.common.typeadapters.WebQueryResponseTypeAdapterFactory;

public class WebQueryResponseTypeAdapterTest {

    private Gson gson;
    
    @Before
    public void setup() {
        gson = new GsonBuilder()
                    .registerTypeAdapterFactory(new PojoTypeAdapterFactory())
                    .registerTypeAdapterFactory(new WebQueryResponseTypeAdapterFactory())
                    .create();
    }
    
    @Test
    public void canSerializeBasic() {
        // Our test pojo
        AgentInformation agentInfo = new AgentInformation("testing");
        agentInfo.setAlive(false);
        AgentInformation[] resultList = new AgentInformation[] {
                agentInfo
        };
        
        // create the query response
        WebQueryResponse<AgentInformation> response = new WebQueryResponse<>();
        response.setResultList(resultList);
        response.setCursorId(300);
        response.setResponseCode(PreparedStatementResponseCode.ILLEGAL_PATCH);
        
        String jsonStr = gson.toJson(response);
        String expectedJson = "{\"errno\":-1,\"cId\":300,\"cHasMore\":false,\"payload\":[{\"agentId\":\"testing\",\"alive\":false,\"startTime\":0,\"stopTime\":0}]}";
        assertEquals(expectedJson, jsonStr);
    }
    
    @Test
    public void canSerializeNoResultList() {
        // create the query response
        WebQueryResponse<AgentInformation> response = new WebQueryResponse<>();
        response.setResultList(null); // should be left out from serialization
        response.setCursorId(-0xdeadbeef);
        response.setResponseCode(PreparedStatementResponseCode.ILLEGAL_PATCH);
        
        String jsonStr = gson.toJson(response);
        String expectedJson = "{\"errno\":-1,\"cId\":559038737,\"cHasMore\":false}";
        assertEquals(expectedJson, jsonStr);
    }
    
    @Test
    public void canDeserializeNoResultList() {
        String rawJson = "{\"errno\":-1,\"cId\":444,\"cHasMore\":true}";
        Type queryResponseType = new TypeToken<WebQueryResponse<AgentInformation>>() {}.getType();
        WebQueryResponse<AgentInformation> actual = gson.fromJson(rawJson, queryResponseType);
        assertEquals(true, actual.hasMoreBatches());
        assertEquals(444, actual.getCursorId());
        
        assertNull(actual.getResultList());
        
        assertEquals(PreparedStatementResponseCode.ILLEGAL_PATCH, actual.getResponseCode());
    }
    
    @Test
    public void canDeserializeBasic() {
        String rawJson = "{\"errno\":-1,\"cId\":444,\"cHasMore\":true,\"payload\":[{\"startTime\":0,\"stopTime\":0,\"alive\":true,\"agentId\":\"testing\"}]}";
        Type queryResponseType = new TypeToken<WebQueryResponse<AgentInformation>>() {}.getType();
        WebQueryResponse<AgentInformation> actual = gson.fromJson(rawJson, queryResponseType);
        assertEquals(true, actual.hasMoreBatches());
        assertEquals(444, actual.getCursorId());
        
        AgentInformation[] actualList = actual.getResultList();
        
        assertEquals(PreparedStatementResponseCode.ILLEGAL_PATCH, actual.getResponseCode());
        assertEquals(1, actualList.length);
        AgentInformation actualInfo = actualList[0];
        assertEquals(true, actualInfo.isAlive());
        assertEquals("testing", actualInfo.getAgentId());
    }
    
    @Test
    public void canSerializeAndDeserializeBasic() {
        // Our test pojo
        AgentInformation agentInfo = new AgentInformation("testing");
        agentInfo.setAlive(false);
        AgentInformation[] resultList = new AgentInformation[] {
                agentInfo
        };
        
        // create the query response
        WebQueryResponse<AgentInformation> response = new WebQueryResponse<>();
        response.setResultList(resultList);
        response.setResponseCode(PreparedStatementResponseCode.ILLEGAL_PATCH);
        
        String jsonStr = gson.toJson(response);

        // We need to tell GSON which parametrized type we want it to deserialize
        // it to.
        Type queryResponseType = new TypeToken<WebQueryResponse<AgentInformation>>() {}.getType();
        WebQueryResponse<AgentInformation> actual = gson.fromJson(jsonStr, queryResponseType);
        
        AgentInformation[] actualList = actual.getResultList();
        
        assertEquals(PreparedStatementResponseCode.ILLEGAL_PATCH, actual.getResponseCode());
        assertEquals(1, actualList.length);
        AgentInformation actualInfo = actualList[0];
        assertEquals(false, actualInfo.isAlive());
        assertEquals("testing", actualInfo.getAgentId());
    }
    
    @Test
    public void canSerializeAndDeserializeVariousPojos() {
        // Our test pojo
        AgentInformation agentInfo = new AgentInformation("testing");
        agentInfo.setAlive(false);
        AgentInformation[] resultList = new AgentInformation[] {
                agentInfo
        };
        
        // create the query response
        WebQueryResponse<AgentInformation> response = new WebQueryResponse<>();
        response.setResultList(resultList);
        response.setResponseCode(PreparedStatementResponseCode.ILLEGAL_PATCH);
        
        String jsonStr = gson.toJson(response);
        String expectedJson = "{\"errno\":-1,\"cId\":0,\"cHasMore\":false,\"payload\":[{\"agentId\":\"testing\",\"alive\":false,\"startTime\":0,\"stopTime\":0}]}";
        assertEquals(expectedJson, jsonStr);

        // We need to tell GSON which parametrized type we want it to deserialize
        // it to.
        Type queryResponseType = new TypeToken<WebQueryResponse<AgentInformation>>() {}.getType();
        WebQueryResponse<AgentInformation> actual = gson.fromJson(jsonStr, queryResponseType);
        
        AgentInformation[] actualList = actual.getResultList();
        
        assertEquals(PreparedStatementResponseCode.ILLEGAL_PATCH, actual.getResponseCode());
        assertEquals(1, actualList.length);
        AgentInformation actualInfo = actualList[0];
        assertEquals(false, actualInfo.isAlive());
        assertEquals("testing", actualInfo.getAgentId());
        
        // Do it again using HostInfo as model
        HostInfo hostInfo = new HostInfo();
        hostInfo.setAgentId("something");
        hostInfo.setCpuCount(56);
        hostInfo.setHostname("flukebox");
        
        HostInfo[] hostInfoResults = new HostInfo[] {
                hostInfo
        };
        
        WebQueryResponse<HostInfo> expected = new WebQueryResponse<>();
        expected.setResultList(hostInfoResults);
        expected.setResponseCode(PreparedStatementResponseCode.QUERY_SUCCESS);
        
        jsonStr = gson.toJson(expected);
        Type hostinfoQueryResponseType = new TypeToken<WebQueryResponse<HostInfo>>() {}.getType();
        WebQueryResponse<HostInfo> actualResp = gson.fromJson(jsonStr, hostinfoQueryResponseType);
        
        assertEquals(PreparedStatementResponseCode.QUERY_SUCCESS, actualResp.getResponseCode());
        HostInfo[] hostInfoList = actualResp.getResultList();
        assertEquals(1, hostInfoList.length);
        assertEquals("something", hostInfoList[0].getAgentId());
        assertEquals(56, hostInfoList[0].getCpuCount());
        assertEquals("flukebox", hostInfoList[0].getHostname());
    }
}

