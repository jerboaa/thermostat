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

package com.redhat.thermostat.web.common;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.Pojo;

public class ThermostatGSONConverterTest {

 private Gson gson;
    
    @Before
    public void setup() {
        gson = new GsonBuilder()
                .registerTypeAdapter(Pojo.class, new ThermostatGSONConverter())
                .create();
    }
    
    @Test
    public void canSerializeDeserializeBasic() {
        // Our test pojo
        AgentInformation agentInfo = new AgentInformation();
        agentInfo.setAgentId("testing");
        agentInfo.setAlive(true);
        
        String jsonStr = gson.toJson(agentInfo);
        
        AgentInformation actual = gson.fromJson(jsonStr, AgentInformation.class);
        
        assertEquals("testing", actual.getAgentId());
        assertEquals(true, actual.isAlive());
    }
    
    @Test
    public void canSerializeDeserializeArray() {
        // Our test pojo
        AgentInformation agentInfo = new AgentInformation();
        agentInfo.setAgentId("testing");
        agentInfo.setAlive(true);
        AgentInformation[] agentInfos = new AgentInformation[] {
                agentInfo
        };
        
        String jsonStr = gson.toJson(agentInfos);
        
        AgentInformation[] actual = gson.fromJson(jsonStr, AgentInformation[].class);
        
        assertEquals("testing", actual[0].getAgentId());
        assertEquals(true, actual[0].isAlive());
    }
}
