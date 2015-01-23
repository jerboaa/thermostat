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

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.redhat.thermostat.storage.core.PreparedParameters;
import com.redhat.thermostat.web.common.SharedStateId;
import com.redhat.thermostat.web.common.WebPreparedStatement;

@SuppressWarnings("rawtypes")
class WebPreparedStatementTypeAdapter extends TypeAdapter<WebPreparedStatement> {

    private static final String PROP_PARAMS = "p";
    private static final String PROP_STMT_ID = "sid";
    
    private final TypeAdapter<SharedStateId> sharedStateTa;
    private final TypeAdapter<PreparedParameters> prepParamsTa;
    
    WebPreparedStatementTypeAdapter(Gson gson) {
        this.sharedStateTa = gson.getAdapter(SharedStateId.class);
        this.prepParamsTa = gson.getAdapter(PreparedParameters.class);
    }
    
    @Override
    public void write(JsonWriter out, WebPreparedStatement value)
            throws IOException {
        // handle null
        if (value == null) {
            out.nullValue();
            return;
        }
        
        out.beginObject();
        
        // statement id
        out.name(PROP_STMT_ID);
        sharedStateTa.write(out, value.getStatementId());

        // prepared parameters
        out.name(PROP_PARAMS);
        prepParamsTa.write(out, value.getParams());
        
        out.endObject();        
    }

    @Override
    public WebPreparedStatement read(JsonReader in) throws IOException {
        // handle null
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        
        in.beginObject();
        
        
        // statement id
        String name = in.nextName();
        if (!name.equals(PROP_STMT_ID)) {
            throw new IllegalStateException("Expected name " + PROP_STMT_ID + " but was " + name);
        }
        SharedStateId id = sharedStateTa.read(in);
        
        // params
        PreparedParameters params = null;
        // params value might be null and missing.
        if (in.peek() == JsonToken.NAME) {
            name = in.nextName();
            if (!name.equals(PROP_PARAMS)) {
                throw new IllegalStateException("Expected name " + PROP_PARAMS + " but was " + name);
            }
            params = prepParamsTa.read(in);
        }
        
        in.endObject();
        
        WebPreparedStatement<?> stmt = new WebPreparedStatement<>();
        stmt.setParams(params);
        stmt.setStatementId(id);
        return stmt;
    }

}
