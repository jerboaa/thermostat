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
 * Old non-stream GSON API serializer. Used only in performance tests. 
 */
public class LegacyPreparedParameterSerializer implements JsonSerializer<PreparedParameter>,
                                                    JsonDeserializer<PreparedParameter>{

    private static final Logger logger = LoggingUtils.getLogger(LegacyPreparedParameterSerializer.class);
    private static final String PROP_TYPE = "type";
    private static final String PROP_VALUE = "value";
    private static final Set<Class<?>> VALID_CLASSES;
    private static final Set<Class<?>> PRIMITIVES_NOT_ALLOWING_NULL_VAL;
    // maps type names to classes:
    //    "int" => int.class , "double" => double.class, "[I" => int[].class
    //    and so on.
    private static final Map<String, Class<?>> CLASSES_LOOKUP_TABLE;
    
    static {
        VALID_CLASSES = new HashSet<>();
        CLASSES_LOOKUP_TABLE = new HashMap<>();
        CLASSES_LOOKUP_TABLE.put(int.class.getName(), int.class);
        CLASSES_LOOKUP_TABLE.put(long.class.getName(), long.class);
        CLASSES_LOOKUP_TABLE.put(boolean.class.getName(), boolean.class);
        CLASSES_LOOKUP_TABLE.put(double.class.getName(), double.class);
        CLASSES_LOOKUP_TABLE.put(String.class.getName(), String.class);
        CLASSES_LOOKUP_TABLE.put(int[].class.getName(), int[].class);
        CLASSES_LOOKUP_TABLE.put(long[].class.getName(), long[].class);
        CLASSES_LOOKUP_TABLE.put(boolean[].class.getName(), boolean[].class);
        CLASSES_LOOKUP_TABLE.put(double[].class.getName(), double[].class);
        CLASSES_LOOKUP_TABLE.put(String[].class.getName(), String[].class);
        VALID_CLASSES.addAll(CLASSES_LOOKUP_TABLE.values());
        PRIMITIVES_NOT_ALLOWING_NULL_VAL = new HashSet<>();
        PRIMITIVES_NOT_ALLOWING_NULL_VAL.add(int.class);
        PRIMITIVES_NOT_ALLOWING_NULL_VAL.add(long.class);
        PRIMITIVES_NOT_ALLOWING_NULL_VAL.add(boolean.class);
        PRIMITIVES_NOT_ALLOWING_NULL_VAL.add(double.class);
    }
    
    @Override
    public JsonElement serialize(PreparedParameter param, Type type,
            JsonSerializationContext ctxt) {
        JsonObject result = new JsonObject();
        JsonElement valueElem = serializeValue(ctxt, param.getValue(), param.getType());
        result.add(PROP_VALUE, valueElem);
        JsonPrimitive typeElem = new JsonPrimitive(param.getType().getName());
        result.add(PROP_TYPE, typeElem);
        return result;
    }

    private JsonElement serializeValue(JsonSerializationContext ctxt, Object value, Class<?> type) {
        JsonElement element;
        // Special case pojo list types: the value class is of array type for
        // them, but the type is the component type.
        if (value != null && value.getClass().isArray() && !type.isArray()) {
            assert(Pojo.class.isAssignableFrom(type));
            Class<?> arrayType = Array.newInstance(type, 0).getClass();
            element = ctxt.serialize(value, arrayType);
        } else {
            element = ctxt.serialize(value, type);
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
        Object value = deserializeValue(ctxt, valueElement, typeVal);
        PreparedParameter param = new PreparedParameter();
        param.setType(typeVal);
        param.setValue(value);
        return param;
    }

    private Class<?> deserializeTypeVal(String className) {
        Class<?> typeVal = null;
        if (CLASSES_LOOKUP_TABLE.containsKey(className)) {
            typeVal = CLASSES_LOOKUP_TABLE.get(className);
        } else {
            try {
                // We need this for Pojo + Pojo list type params. For pojo
                // lists the name we get passed is the component type of the
                // array.
                typeVal = Class.forName(className);
            } catch (ClassNotFoundException e) {
                logger.log(Level.WARNING, "Failed to resolve class type for '"
                        + className + "'.");
            }
        }
        return typeVal;
    }

    private Object deserializeValue(JsonDeserializationContext ctxt,
            JsonElement valueElement, Class<?> valType) {
        // special case for Pojo/Pojo list types. In that case, the valType
        // is the component type for arrays. In order to distinguish pojo
        // lists from pojos, we use JSON's array info about the value element.
        if (valueElement != null && valueElement.isJsonArray() && !valType.isArray()) {
            assert(Pojo.class.isAssignableFrom(valType));
            Class<?> arrayType = Array.newInstance(valType, 0).getClass();
            return ctxt.deserialize(valueElement, arrayType);
        } else {
            Object value = ctxt.deserialize(valueElement, valType);
            validatePrimitivesForNull(value, valType);
            return value;
        }
    }

    private void validatePrimitivesForNull(Object value, Class<?> valType) {
        if (PRIMITIVES_NOT_ALLOWING_NULL_VAL.contains(valType) && value == null) {
            // illegal value for primitive type. according to JLS spec they all
            // have default values and are never null.
            throw new IllegalStateException( valType + " primitive" +
                                            " does not accept a null value!");
        }
    }

    // Allow valid classes + Pojo types, refuse everything else
    private void validateSaneClassName(Class<?> clazz) {
        // isAssignableFrom throws NPE if clazz is null.
        if (VALID_CLASSES.contains(clazz) ||
                Pojo.class.isAssignableFrom(clazz)) {
            return;
        }
        throw new IllegalStateException("Illegal type of parameter " + clazz.getCanonicalName());
    }

}
