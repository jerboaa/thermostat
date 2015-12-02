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

package com.redhat.thermostat.dev.populator.config.typeadapter;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.redhat.thermostat.dev.populator.config.ConfigItem;
import com.redhat.thermostat.dev.populator.config.PopulationConfig;
import com.redhat.thermostat.dev.populator.dependencies.Relationship;

public class PopulationConfigTypeAdapterTest {

    private Gson gson;
    
    @Before
    public void setup() {
        gson= new GsonBuilder()
                .registerTypeAdapter(Relationship.class, new RelationShipTypeAdapter())
                .registerTypeAdapter(ConfigItem.class, new ConfigItemTypeAdapter())
                .registerTypeAdapterFactory(new PopulationConfigTypeAdapterFactory())
                .create();
    }
    
    @Test
    public void canParseSimpleNoRelationships() throws Exception {
        String json = "{ \"records\": " +
                "[{ \"name\": \"config-item\", \"number\": 3, \"alive\": 1}," +
                "{ \"name\": \"agent-config\", \"number\": 10, \"alive\": 3 }]" +
                "}";
        PopulationConfig config = gson.fromJson(json, PopulationConfig.class);
        ConfigItem item = config.getConfig("config-item");
        assertEquals(3, item.getNumber());
        assertEquals(1, item.getAliveItems());
        assertEquals("config-item", item.getName());
        item = config.getConfig("agent-config");
        assertEquals(10, item.getNumber());
        assertEquals(3, item.getAliveItems());
        assertEquals("agent-config", item.getName());
    }
    
    @Test
    public void canParseSimpleWithRelationships() throws Exception {
        String json = "{ \"records\": " +
                "[{ \"name\": \"config-item\", \"number\": 3, \"alive\": 1}," +
                "{ \"name\": \"agent-config\", \"number\": 10, \"alive\": 3 }]," +
                " \"relationships\": "+
                "[{ \"from\": \"config-item\", \"to\": \"agent-config\", \"key\": \"foo\"}]" +
                "}";
        PopulationConfig config = gson.fromJson(json, PopulationConfig.class);
        ConfigItem item = config.getConfig("config-item");
        assertEquals(3, item.getNumber());
        assertEquals(1, item.getAliveItems());
        assertEquals("config-item", item.getName());
        item = config.getConfig("agent-config");
        assertEquals(10, item.getNumber());
        assertEquals(3, item.getAliveItems());
        assertEquals("agent-config", item.getName());
        // Do something that uses relationships
        List<ConfigItem> sorted = config.getConfigsTopologicallySorted();
        ConfigItem first = sorted.get(0);
        assertEquals("config-item", first.getName());
    }
}
