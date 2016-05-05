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

package com.redhat.thermostat.storage.populator.internal.config.typeadapter;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.redhat.thermostat.storage.populator.internal.config.ConfigItem;
import com.redhat.thermostat.storage.populator.internal.config.typeadapter.ConfigItemTypeAdapter;

public class ConfigItemTypeAdapterTest {

    private Gson gson;
    
    @Before
    public void setup() {
        gson= new GsonBuilder()
                .registerTypeAdapter(ConfigItem.class, new ConfigItemTypeAdapter())
                .create();
    }
    
    @Test
    public void testBasicNonNull() {
        ConfigItem item = gson.fromJson("{ \"name\": \"config-item\", \"number\": 3, \"alive\": 1}", ConfigItem.class);
        assertEquals("config-item", item.getName());
        assertEquals(3, item.getNumber());
        assertEquals(1, item.getAliveItems());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testBasicNegativeNumber() {
        gson.fromJson("{ \"name\": \"config-item\", \"number\": -3, \"alive\": 1}", ConfigItem.class);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testBasicNegativeAlive() {
        gson.fromJson("{ \"name\": \"config-item\", \"number\": 10, \"alive\": -1}", ConfigItem.class);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testBasicAliveGreaterThanNumber() {
        // alive > number. Expected strict subset
        gson.fromJson("{ \"name\": \"config-item\", \"number\": 10, \"alive\": 11}", ConfigItem.class);
    }
    
    @Test
    public void testBasicNullAlive() {
        ConfigItem item = gson.fromJson("{ \"name\": \"config-item\", \"number\": 3}", ConfigItem.class);
        assertEquals("config-item", item.getName());
        assertEquals(3, item.getNumber());
        assertEquals(ConfigItem.UNSET, item.getAliveItems());
    }
    
    @Test
    public void testArray() {
        ConfigItem[] items = gson.fromJson("[{ \"name\": \"config-item\", \"number\": 3, \"alive\": 1}," +
                "{ \"name\": \"agent-config\", \"number\": 10, \"alive\": 3 }]", ConfigItem[].class);
        assertEquals("config-item", items[0].getName());
        assertEquals(3, items[0].getNumber());
        assertEquals(1, items[0].getAliveItems());
        assertEquals("agent-config", items[1].getName());
        assertEquals(10, items[1].getNumber());
        assertEquals(3, items[1].getAliveItems());
    }
}
