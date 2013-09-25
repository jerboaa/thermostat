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

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.PreparedParameter;
import com.redhat.thermostat.storage.model.Pojo;

/**
 * GSON type adapter for {@link PreparedParameter}.
 *
 */
public class PreparedParameterSerializer implements JsonDeserializer<PreparedParameter>, JsonSerializer<PreparedParameter>{

    private static final Logger logger = LoggingUtils.getLogger(PreparedParameterSerializer.class);
    private static final String PROP_TYPE = "type";
    private static final String PROP_IS_ARRAY_TYPE = "isArray";
    private static final String PROP_VALUE = "value";
    private static final Set<Class<?>> WRAPPER_CLASSES;
    // maps wrapper classes to primitives:
    //    Integer.class => int.class , Double.class => double.class, etc.
    private static final Map<Class<?>, Class<?>> TO_PRIMITIVE_ARRAY_MAP;
    
    static {
        WRAPPER_CLASSES = new HashSet<>();
        TO_PRIMITIVE_ARRAY_MAP = new HashMap<>();
        WRAPPER_CLASSES.add(Integer.class);
        TO_PRIMITIVE_ARRAY_MAP.put(Integer.class, int.class);
        WRAPPER_CLASSES.add(Long.class);
        TO_PRIMITIVE_ARRAY_MAP.put(Long.class, long.class);
        WRAPPER_CLASSES.add(Boolean.class);
        TO_PRIMITIVE_ARRAY_MAP.put(Boolean.class, boolean.class);
        WRAPPER_CLASSES.add(Double.class);
        TO_PRIMITIVE_ARRAY_MAP.put(Double.class, double.class);
        
    }
    
    @Override
    public JsonElement serialize(PreparedParameter param, Type type,
            JsonSerializationContext ctxt) {
        JsonObject result = new JsonObject();
        JsonElement valueElem = serializeValue(ctxt, param.getValue(), param.getType(), param.isArrayType());
        result.add(PROP_VALUE, valueElem);
        JsonPrimitive typeElem = new JsonPrimitive(param.getType().getName());
        result.add(PROP_TYPE, typeElem);
        JsonPrimitive arrayType = new JsonPrimitive(param.isArrayType());
        result.add(PROP_IS_ARRAY_TYPE, arrayType);
        return result;
    }

    private JsonElement serializeValue(JsonSerializationContext ctxt, Object value, Class<?> compType, boolean isArray) {
        JsonElement element;
        if (isArray) {
                Class<?> arrayType = Array.newInstance(compType, 0).getClass();
                element = ctxt.serialize(value, arrayType);
        } else {
            element = ctxt.serialize(value, compType);
        }
        return element;
    }

    @Override
    public PreparedParameter deserialize(JsonElement jsonElem, Type type,
            JsonDeserializationContext ctxt) throws JsonParseException {
        JsonElement typeElem = jsonElem.getAsJsonObject().get(PROP_TYPE);
        String className = typeElem.getAsString();
        // perform some sanity checking on the types of classes we actually
        // de-serialize
        Class<?> typeVal = deserializeTypeVal(className);
        validateSaneClassName(typeVal);
        JsonElement valueElement = jsonElem.getAsJsonObject().get(PROP_VALUE);
        JsonElement isArrayElement = jsonElem.getAsJsonObject().get(PROP_IS_ARRAY_TYPE);
        boolean isArray = isArrayElement.getAsBoolean();
        Object value = deserializeValue(ctxt, valueElement, typeVal, isArray);
        PreparedParameter param = new PreparedParameter();
        param.setType(typeVal);
        param.setValue(value);
        param.setArrayType(isArray);
        return param;
    }

    private Class<?> deserializeTypeVal(String className) {
        Class<?> typeVal = null;
        try {
            typeVal = Class.forName(className);
        } catch (ClassNotFoundException e) {
            logger.log(Level.WARNING, "Failed to resolve class type for '"
                    + className + "'.");
        }
        ;
        return typeVal;
    }

    private Object deserializeValue(JsonDeserializationContext ctxt,
            JsonElement valueElement, Class<?> valType, boolean isArray) {
        if (!isArray) {
            // By telling GSON the type, we get the correctly casted
            // value back.
            return ctxt.deserialize(valueElement, valType);
        } else {
            Class<?> arrayType = Array.newInstance(valType, 0).getClass();
            Object array;
            // Make sure we get primitive type arrays if this is an array type
            // of one of the wrapped primitives.
            if (WRAPPER_CLASSES.contains(valType)) {
                Class<?> primType = Array.newInstance(TO_PRIMITIVE_ARRAY_MAP.get(valType), 0).getClass();
                array = ctxt.deserialize(valueElement, primType);
            } else {
                array = ctxt.deserialize(valueElement, arrayType);
            }
            return array;
        }
    }

    // Allow wrapper classes, String + Pojo types, refuse everything else
    private void validateSaneClassName(Class<?> clazz) {
        if (WRAPPER_CLASSES.contains(clazz) ||
                String.class == clazz ||
                Pojo.class.isAssignableFrom(clazz)) {
            return;
        }
        throw new IllegalStateException("Illegal type of parameter " + clazz.getCanonicalName());
    }

}
