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

package com.redhat.thermostat.web.common;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.redhat.thermostat.storage.model.Pojo;

/**
 * GSON type adapter for {@link QueryResponse}.
 * Depends on {@link ThermostatGSONConverter} being registered as
 * type adapter as well.
 *
 */
public class WebQueryResponseSerializer<T extends Pojo> implements
        JsonDeserializer<WebQueryResponse<T>>, JsonSerializer<WebQueryResponse<T>> {

    private static final String PROP_RESULT = "payload";
    private static final String PROP_ERROR_CODE = "errno";
    
    @Override
    public JsonElement serialize(WebQueryResponse<T> qResponse, Type type,
            JsonSerializationContext ctxt) {
        JsonObject result = new JsonObject();
        JsonElement resultsElem = ctxt.serialize(qResponse.getResultList());
        result.add(PROP_RESULT, resultsElem);
        JsonPrimitive errnoElem = new JsonPrimitive(qResponse.getResponseCode());
        result.add(PROP_ERROR_CODE, errnoElem);
        return result;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public WebQueryResponse<T> deserialize(JsonElement jsonElem, Type type,
            JsonDeserializationContext ctxt) throws JsonParseException {
        // fromJson() calls need to pass in the right *parameterized* type token:
        // example for AgentInformation as T:
        //   Type queryResponseType = new TypeToken<WebQueryResponse<AgentInformation>>() {}.getType();
        //   gson.fromJson(jsonStr, queryResponseType)
        Type[] typeParameters = ((ParameterizedType)type).getActualTypeArguments();
        Type queryResponseTypeParam = typeParameters[0]; // WebQueryResponse has only one parameterized type T
        JsonArray resultElem = jsonElem.getAsJsonObject().get(PROP_RESULT).getAsJsonArray();
        @SuppressWarnings("rawtypes")
        Class typeOfGeneric = (Class)queryResponseTypeParam;
        T[] array = (T[])Array.newInstance(typeOfGeneric, resultElem.size());
        for (int i = 0; i < resultElem.size(); i++) {
            array[i] = ctxt.deserialize(resultElem.get(i), queryResponseTypeParam);
        }
        JsonElement errorCodeElem = jsonElem.getAsJsonObject().get(PROP_ERROR_CODE);
        int errorCode = ctxt.deserialize(errorCodeElem, int.class);
        WebQueryResponse<T> qResponse = new WebQueryResponse<>();
        qResponse.setResponseCode(errorCode);
        qResponse.setResultList(array);
        return qResponse;
    }
    
}

