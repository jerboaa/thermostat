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

package com.redhat.thermostat.web.common.typeadapters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.redhat.thermostat.web.common.SharedStateId;
import com.redhat.thermostat.web.common.WebPreparedStatementResponse;

public class WebPreparedStatementResponseTypeAdapterTest {

    private Gson gson;
    
    @Before
    public void setup() {
        gson = new GsonBuilder()
                .registerTypeAdapterFactory(new SharedStateIdTypeAdapterFactory())
                .registerTypeAdapterFactory(new WebPreparedStatementResponseTypeAdapterFactory())
                .create();
    }
    
    @Test
    public void testSerializationDeserializationBasic() {
        WebPreparedStatementResponse response = new WebPreparedStatementResponse();
        response.setNumFreeVariables(6);
        
        UUID uuid = UUID.randomUUID();
        SharedStateId stmtId = new SharedStateId(WebPreparedStatementResponse.ILLEGAL_STATEMENT, uuid);
        response.setStatementId(stmtId);
        
        String jsonStr = gson.toJson(response, WebPreparedStatementResponse.class);
        String expectedString = "{\"numFreeVars\":6,\"stmtId\":{\"sid\":-1,\"stok\":\""+ uuid.toString() + "\"}}";
        assertEquals(expectedString, jsonStr);
        
        WebPreparedStatementResponse actual = gson.fromJson(jsonStr, WebPreparedStatementResponse.class);
        
        assertEquals(6, actual.getNumFreeVariables());
        assertEquals(stmtId, actual.getStatementId());
    }
    
    public void deserializeNull() {
        String jsonString = "{\"numFreeVars\":11}";
        WebPreparedStatementResponse resp = gson.fromJson(jsonString, WebPreparedStatementResponse.class);
        assertNull(resp.getStatementId());
        assertEquals(11, resp.getNumFreeVariables());
    }
    
    @Test
    public void canDeserializeBasic() {
        UUID uuid = UUID.randomUUID();
        SharedStateId stmtId = new SharedStateId(6, uuid);
        String jsonString = "{\"numFreeVars\":11,\"stmtId\":{\"sid\":6,\"stok\":\""+ uuid.toString() + "\"}}";
        WebPreparedStatementResponse actual = gson.fromJson(jsonString, WebPreparedStatementResponse.class);
        assertEquals(stmtId, actual.getStatementId());
        assertEquals(11, actual.getNumFreeVariables());
    }
    
    @Test(expected=JsonSyntaxException.class)
    public void testDeserializationFail() {
        String invalidString = "{\"forbar\":6,\"stmtId\":-1}";
        gson.fromJson(invalidString, WebPreparedStatementResponse.class);
    }
    
    @Test
    public void canSerializeDeserializeNull() {
        WebPreparedStatementResponse response = null;
        assertNull(response);
        String json = gson.toJson(response, WebPreparedStatementResponse.class);
        assertEquals("null", json);
        WebPreparedStatementResponse actual = gson.fromJson(json, WebPreparedStatementResponse.class);
        assertNull(actual);
    }
}

