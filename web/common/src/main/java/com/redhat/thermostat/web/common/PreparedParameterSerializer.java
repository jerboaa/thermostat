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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.redhat.thermostat.storage.core.PreparedParameter;


/**
 * GSON type adapter for {@link PreparedParameter}.
 *
 */
public class PreparedParameterSerializer implements JsonDeserializer<PreparedParameter>, JsonSerializer<PreparedParameter>{

    private static final String PROP_TYPE = "type";
    private static final String PROP_VALUE = "value";
    // The set of valid classes for types
    private static final Set<String> VALID_CLASSNAMES;
    
    static {
        VALID_CLASSNAMES = new HashSet<>();
        VALID_CLASSNAMES.add(String.class.getCanonicalName());
        VALID_CLASSNAMES.add(String[].class.getCanonicalName());
        VALID_CLASSNAMES.add(Integer.class.getCanonicalName());
        VALID_CLASSNAMES.add(Long.class.getCanonicalName());
        VALID_CLASSNAMES.add(Boolean.class.getCanonicalName());
    }
    
    @Override
    public JsonElement serialize(PreparedParameter param, Type type,
            JsonSerializationContext ctxt) {
        JsonObject result = new JsonObject();
        JsonElement valueElem = serializeValue(param.getValue());
        result.add(PROP_VALUE, valueElem);
        JsonPrimitive typeElem = new JsonPrimitive(param.getType().getCanonicalName());
        result.add(PROP_TYPE, typeElem);
        return result;
    }

    private JsonElement serializeValue(Object value) {
        JsonElement element;
        if (value instanceof Integer) {
            int val = ((Integer)value).intValue();
            element = new JsonPrimitive(val);
        } else if (value instanceof Long) {
            long val = ((Long)value).longValue();
            element = new JsonPrimitive(val);
        } else if (value instanceof String) {
            String val = (String)value;
            element = new JsonPrimitive(val);
        } else if (value instanceof String[]) {
            String[] val = (String[])value;
            JsonArray array = new JsonArray();
            for (int i = 0; i < val.length; i++) {
                array.add(new JsonPrimitive(val[i]));
            }
            element = array;
        } else if (value instanceof Boolean) {
            Boolean val = (Boolean)value;
            element = new JsonPrimitive(val.booleanValue());
        } else {
            throw new IllegalStateException("Unexpected value for serialization '" + value + "'");
        }
        return element;
    }

    @Override
    public PreparedParameter deserialize(JsonElement jsonElem, Type type,
            JsonDeserializationContext ctxt) throws JsonParseException {
        JsonElement typeElem = jsonElem.getAsJsonObject().get(PROP_TYPE);
        String className = typeElem.getAsString();
        // perform some sanity checking on which classes we do forName() :)
        validateSaneClassName(className);
        Class<?> typeVal = deserializeTypeVal(className);
        JsonElement valueElement = jsonElem.getAsJsonObject().get(PROP_VALUE);
        Object value = deserializeValue(ctxt, valueElement, typeVal);
        PreparedParameter param = new PreparedParameter();
        param.setType(typeVal);
        param.setValue(value);
        return param;
    }

    private Class<?> deserializeTypeVal(String className) {
        Class<?> typeVal = null;
        if (className.equals(String[].class.getCanonicalName())) {
            typeVal = String[].class;
        } else {
            try {
                typeVal = Class.forName(className);
            } catch (ClassNotFoundException ignored) {
                // we only load valid classes that way
            };
        }
        return typeVal;
    }

    private Object deserializeValue(JsonDeserializationContext ctxt,
            JsonElement valueElement, Class<?> valType) {
        if (valueElement.isJsonPrimitive()) {
            // By telling GSON the type, we get the rightly casted
            // value back.
            return ctxt.deserialize(valueElement, valType);
        } else if (valueElement.isJsonArray()) {
            // Only string arrays are supported
            List<String> values = new ArrayList<>();
            JsonArray jsonArray = (JsonArray)valueElement;
            Iterator<JsonElement> it = jsonArray.iterator();
            while (it.hasNext()) {
                JsonElement elem = it.next();
                String strElem = ctxt.deserialize(elem, String.class);
                values.add(strElem);
            }
            return values.toArray(new String[0]);
        } else {
            throw new IllegalStateException("Illegal json for parameter value");
        }
    }

    private void validateSaneClassName(String className) {
        if (!VALID_CLASSNAMES.contains(className)) {
            throw new IllegalStateException("Illegal type of parameter " + className);
        }
    }

}
