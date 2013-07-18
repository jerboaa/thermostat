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
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.redhat.thermostat.storage.core.PreparedParameter;
import com.redhat.thermostat.storage.core.PreparedParameters;

public class WebPreparedStatementSerializerTest {

    private Gson gson;
    
    @Before
    public void setup() {
        gson = new GsonBuilder()
                .registerTypeAdapter(WebPreparedStatement.class,
                        new WebPreparedStatementSerializer())
                .registerTypeAdapter(PreparedParameter.class,
                        new PreparedParameterSerializer()).create();
    }
    
    @Test
    public void canSerializeAndDeserialize() {
        PreparedParameters params = new PreparedParameters(5);
        params.setInt(0, 2);
        params.setString(1, "testing");
        params.setLong(2, 222L);
        params.setStringList(3, new String[] { "one", "two" });
        params.setBoolean(4, true);
        WebPreparedStatement<?> stmt = new WebPreparedStatement<>();
        stmt.setParams(params);
        stmt.setStatementId(WebPreparedStatementResponse.DESCRIPTOR_PARSE_FAILED);
        String jsonString = gson.toJson(stmt, WebPreparedStatement.class);
        WebPreparedStatement<?> newStmt = gson.fromJson(jsonString, WebPreparedStatement.class);
        assertNotNull(newStmt);
        PreparedParameters newParams = newStmt.getParams();
        PreparedParameter[] parameters = newParams.getParams();
        assertEquals(5, parameters.length);
        assertEquals(2, parameters[0].getValue());
        assertEquals(Integer.class, parameters[0].getType());
        assertEquals("testing", parameters[1].getValue());
        assertEquals(String.class, parameters[1].getType());
        assertEquals(222L, parameters[2].getValue());
        assertEquals(Long.class, parameters[2].getType());
        String[] list = (String[])parameters[3].getValue();
        assertEquals(2, list.length);
        assertEquals("one", list[0]);
        assertEquals("two", list[1]);
        assertEquals(String[].class, parameters[3].getType());
        assertEquals(true, parameters[4].getValue());
        assertEquals(Boolean.class, parameters[4].getType());
        assertEquals(WebPreparedStatementResponse.DESCRIPTOR_PARSE_FAILED, newStmt.getStatementId());
    }
}
