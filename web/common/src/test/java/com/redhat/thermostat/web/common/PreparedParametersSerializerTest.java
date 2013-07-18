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
import com.redhat.thermostat.storage.core.PreparedParameter;
import com.redhat.thermostat.storage.core.PreparedParameters;

public class PreparedParametersSerializerTest {

    private Gson gson;
    
    @Before
    public void setup() {
        gson = new GsonBuilder().registerTypeAdapter(
                PreparedParameter.class,
                new PreparedParameterSerializer()).create();
    }
    
    @Test
    public void canSerializeDeserialize() {
        PreparedParameters params = new PreparedParameters(5);
        params.setBoolean(0, true);
        params.setInt(1, 2300);
        params.setLong(2, 2200000000L);
        params.setString(3, "testing");
        String[] list = new String[] {
          "a", "b", "c"      
        };
        params.setStringList(4, list);
        
        String jsonStr = gson.toJson(params, PreparedParameters.class);
        PreparedParameters actualParams = gson.fromJson(jsonStr, PreparedParameters.class);
        
        PreparedParameter[] expected = params.getParams();
        PreparedParameter[] actual = actualParams.getParams();
        
        // last element is the string array, which we check manually
        for (int i = 0; i < expected.length - 1; i++) {
            assertEquals(expected[i].getType(), actual[i].getType());
            assertEquals(expected[i].getValue(), actual[i].getValue());
        }
        String actualList[] = (String[])actual[4].getValue();
        for (int i = 0; i < list.length; i++) {
            assertEquals(list[i], actualList[i]);
        }
    }
}
