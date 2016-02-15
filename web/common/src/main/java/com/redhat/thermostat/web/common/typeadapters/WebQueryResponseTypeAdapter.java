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

package com.redhat.thermostat.web.common.typeadapters;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.web.common.WebQueryResponse;

class WebQueryResponseTypeAdapter<T extends Pojo> extends TypeAdapter<WebQueryResponse<T>> {

    private static final String PROP_RESULT = "payload";
    private static final String PROP_ERROR_CODE = "errno";
    private static final String PROP_CURSOR_ID = "cId";
    private static final String PROP_CURSOR_HAS_MORE_BATCHES = "cHasMore";
    
    // The runtime type of the Pojo
    private final Class<T> runtimePojoType;
    private final Gson gson;
    
    WebQueryResponseTypeAdapter(Type parameterizedType, Gson gson) {
        this.runtimePojoType = extractPojoImplClassFromType(parameterizedType);
        this.gson = gson;
    }
    
    @Override
    public void write(JsonWriter out, WebQueryResponse<T> value)
            throws IOException {
        // handle null
        if (value == null) {
            out.nullValue();
            return;
        }
        
        out.beginObject();
        
        // response code
        out.name(PROP_ERROR_CODE);
        out.value(value.getResponseCode());
        
        // cursor id
        out.name(PROP_CURSOR_ID);
        out.value(value.getCursorId());
        
        // has more batches property
        out.name(PROP_CURSOR_HAS_MORE_BATCHES);
        out.value(value.hasMoreBatches());
        
        // payload
        out.name(PROP_RESULT);
        if (value.getResultList() == null) {
            out.nullValue();
        } else {
            @SuppressWarnings("unchecked")
            TypeAdapter<T[]> pojoTa = (TypeAdapter<T[]>)gson.getAdapter(value.getResultList().getClass());
            pojoTa.write(out, value.getResultList());
        }
        
        out.endObject();
    }

    @Override
    public WebQueryResponse<T> read(JsonReader in) throws IOException {
        // handle null
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        
        in.beginObject();
        
        // response code (can't be null)
        int responseCode = 0;
        String name = in.nextName();
        responseCode = in.nextInt();
        
        // cursor ID (can't be null)
        name = in.nextName();
        int cursorId = in.nextInt();
        
        // Has more batches, boolean, can't be null
        name = in.nextName();
        boolean hasMoreBatches = in.nextBoolean();
        
        if (runtimePojoType == null) {
            throw new IllegalStateException("Runtime pojo type unknown");
        }
        // Result list may be null. If it's there we get a name, otherwise
        // we are done deserializing
        T[] resultList = null;
        if (in.peek() == JsonToken.NAME) {
            name = in.nextName();
            if (name.equals(PROP_RESULT)) {
                @SuppressWarnings("unchecked")
                T[] arrayType = (T[])Array.newInstance(runtimePojoType, 0);
                @SuppressWarnings("unchecked")
                Class<T[]> type = (Class<T[]>)arrayType.getClass();
                TypeAdapter<T[]> pojoTa = gson.getAdapter(type);
                resultList = pojoTa.read(in);
            } else {
                throw new IllegalStateException("Expected " + PROP_RESULT + " but got " + name);
            }
        }
        
        in.endObject();
        
        WebQueryResponse<T> qResponse = new WebQueryResponse<>();
        qResponse.setResponseCode(responseCode);
        qResponse.setCursorId(cursorId);
        qResponse.setHasMoreBatches(hasMoreBatches);
        qResponse.setResultList(resultList);
        return qResponse;
    }
    
    private Class<T> extractPojoImplClassFromType(Type type) {
        if (type instanceof ParameterizedType) {
            // fromJson() calls need to pass in the right *parameterized* type token:
            // example for AgentInformation as T:
            //   Type queryResponseType = new TypeToken<WebQueryResponse<AgentInformation>>() {}.getType();
            //   gson.fromJson(jsonStr, queryResponseType)
            Type[] typeParameters = ((ParameterizedType)type).getActualTypeArguments();
            Type queryResponseTypeParam = typeParameters[0]; // WebQueryResponse has only one parameterized type T
            Class<?> pojoClass = (Class<?>)queryResponseTypeParam;
            if (!Pojo.class.isAssignableFrom(pojoClass)) {
                throw new IllegalStateException("WebQueryResponse parametrized with non-pojo class: " + pojoClass);
            } else {
                @SuppressWarnings("unchecked") // We've just verified it's Pojo assignable.
                Class<T> retval = (Class<T>)pojoClass;
                return retval;
            }
        }
        // should-be: write() will be called and only write().
        return null;
    }

}
