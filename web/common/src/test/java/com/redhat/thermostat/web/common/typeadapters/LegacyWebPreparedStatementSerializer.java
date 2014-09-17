/*
 * Copyright 2012-2014 Red Hat, Inc.
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

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.redhat.thermostat.storage.core.PreparedParameters;
import com.redhat.thermostat.web.common.WebPreparedStatement;

/**
 * Old non-stream GSON API serializer. Used only in performance tests.
 *
 */
public class LegacyWebPreparedStatementSerializer implements
        JsonDeserializer<WebPreparedStatement<?>>,
        JsonSerializer<WebPreparedStatement<?>> {
    
    private static final String PROP_PARAMS = "p";
    private static final String PROP_STMT_ID = "sid";

    @Override
    public JsonElement serialize(WebPreparedStatement<?> stmt, Type type,
            JsonSerializationContext ctxt) {
        JsonObject result = new JsonObject();
        JsonElement parameters = ctxt.serialize(stmt.getParams(), PreparedParameters.class);
        result.add(PROP_PARAMS, parameters);
        JsonPrimitive stmtIdElem = new JsonPrimitive(stmt.getStatementId());
        result.add(PROP_STMT_ID, stmtIdElem);
        return result;
    }

    @Override
    public WebPreparedStatement<?> deserialize(JsonElement jsonElem, Type type,
            JsonDeserializationContext ctxt) throws JsonParseException {
        JsonElement paramsElem = jsonElem.getAsJsonObject().get(PROP_PARAMS);
        JsonElement stmtIdElem = jsonElem.getAsJsonObject().get(PROP_STMT_ID);
        PreparedParameters params = ctxt.deserialize(paramsElem, PreparedParameters.class);
        int stmtId = ctxt.deserialize(stmtIdElem, int.class);
        WebPreparedStatement<?> stmt = new WebPreparedStatement<>();
        stmt.setStatementId(stmtId);
        stmt.setParams(params);
        return stmt;
    }

}

