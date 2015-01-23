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
import java.util.UUID;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.redhat.thermostat.web.common.SharedStateId;

class SharedStateIdTypeAdapter extends TypeAdapter<SharedStateId> {

    private static final String PROP_STMT_ID = "sid";
    private static final String PROP_SERVER_TOKEN = "stok";
    
    @Override
    public void write(JsonWriter out, SharedStateId value) throws IOException {
        // handle null
        if (value == null) {
            out.nullValue();
            return;
        }
        
        out.beginObject();
        
        // statement id
        out.name(PROP_STMT_ID);
        out.value(value.getId());
        
        // server token, may be null
        if (value.getServerToken() != null) {
            out.name(PROP_SERVER_TOKEN);
            out.value(value.getServerToken().toString());
        } else {
            out.name(PROP_SERVER_TOKEN);
            out.nullValue();
        }
        
        out.endObject();
        
    }

    @Override
    public SharedStateId read(JsonReader in) throws IOException {
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
        int stmtId = in.nextInt();
        
        UUID serverToken = null;
        if (in.peek() == JsonToken.NAME) {
            name = in.nextName();
            if (!name.equals(PROP_SERVER_TOKEN)) {
                throw new IllegalStateException("Expected name " + PROP_SERVER_TOKEN + " but was " + name);
            }
            String sToken = in.nextString();
            serverToken = UUID.fromString(sToken);
        }
        in.endObject();
        
        return new SharedStateId(stmtId, serverToken);
    }


}
