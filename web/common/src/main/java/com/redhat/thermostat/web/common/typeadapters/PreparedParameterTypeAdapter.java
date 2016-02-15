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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.redhat.thermostat.storage.core.PreparedParameter;
import com.redhat.thermostat.storage.model.Pojo;

/**
 * GSON type adapter for {@link PreparedParameter}.
 *
 */
class PreparedParameterTypeAdapter extends TypeAdapter<PreparedParameter> {

    private static final String PROP_TYPE = "type";
    private static final String PROP_VALUE = "value";
    private static final Set<Class<?>> VALID_CLASSES;
    private static final Set<Class<?>> PRIMITIVES_NOT_ALLOWING_NULL_VAL;
    private static final Set<Class<?>> BASIC_TYPES;
    // maps type names to classes:
    //    "int" => int.class , "double" => double.class, "[I" => int[].class
    //    and so on.
    private static final Map<String, Class<?>> CLASSES_LOOKUP_TABLE;
    
    private final Gson gson;
    
    static {
        VALID_CLASSES = new HashSet<>();
        BASIC_TYPES = new HashSet<>();
        BASIC_TYPES.add(int.class);
        BASIC_TYPES.add(long.class);
        BASIC_TYPES.add(boolean.class);
        BASIC_TYPES.add(double.class);
        BASIC_TYPES.add(String.class);
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
    
    
    PreparedParameterTypeAdapter(Gson gson) {
        this.gson = gson;
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
                throw new IllegalStateException("Failed to resolve class type for '"
                        + className + "'.");
            }
        }
        validateSaneClassName(typeVal);
        return typeVal;
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

    @Override
    public PreparedParameter read(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        reader.beginObject();
        
        Class<?> type = readType(reader);
        
        assert(type != null);
        assert(Pojo.class.isAssignableFrom(type) || VALID_CLASSES.contains(type));
        
        Object val = readValue(reader, type);
        reader.endObject();
        
        PreparedParameter param = new PreparedParameter();
        param.setValue(val);
        param.setType(type);
        return param;
    }
    
    private Class<?> readType(JsonReader reader) throws IOException {
        String name = reader.nextName();
        if (!name.equals(PROP_TYPE)) {
            throw new IllegalStateException("Expected " + PROP_VALUE + " but was " + name);
        }
        String className = reader.nextString();
        return deserializeTypeVal(className);
    }

    private Object readValue(JsonReader reader, Class<?> valType) throws IOException {
        // values may be null. In that case they are missing from the JSON
        // string. Be sure to handle that case early.
        if (reader.peek() == JsonToken.END_OBJECT) {
            return null; // null parameter value
        }
        String name = reader.nextName();
        if (!name.equals(PROP_VALUE)) {
            throw new IllegalStateException("Expected " + PROP_VALUE + " but was " + name);
        }
        
        JsonToken token = reader.peek();
        if (token == JsonToken.NULL) {
            reader.nextNull();
            // Be sure to not allow null values for primitives. Note:
            // valType may be an array type for which null is fine.
            validatePrimitivesForNull(null, valType);
            return null; // null value
        }
        
        // special case for Pojo/Pojo list types. In that case, the valType
        // is the component type for arrays. In order to distinguish pojo
        // lists from pojos, we use JSON's array info about the value element.
        if (token == JsonToken.BEGIN_ARRAY && !valType.isArray()) {
            assert(Pojo.class.isAssignableFrom(valType));
            Class<?> arrayType = Array.newInstance(valType, 0).getClass();
            TypeAdapter<?> pojoArrayTa = gson.getAdapter(arrayType);
            Object val = pojoArrayTa.read(reader);
            return val;
        } else {
            TypeAdapter<?> nonPojoTypeAdapter = gson.getAdapter(valType);
            Object val = nonPojoTypeAdapter.read(reader);
            validatePrimitivesForNull(val, valType);
            return val;
        }
    }


    @Override
    public void write(JsonWriter writer, PreparedParameter param)
            throws IOException {
        if (param == null) {
            writer.nullValue();
            return;
        }
        writer.beginObject();
        
        // serialize type which must not be null
        writer.name(PROP_TYPE);
        if (param.getType() == null) {
            throw new IllegalStateException("Type of prepared parameter must not be null");
        }
        String typeStr = param.getType().getName();
        writer.value(typeStr);
        
        // serialize value
        writer.name(PROP_VALUE);
        serializeValue(writer, param);
        
        writer.endObject();
    }
    
    private void serializeValue(JsonWriter writer, PreparedParameter param) throws IOException {
        if (param.getValue() == null) {
            writer.nullValue();
            return;
        }
        
        Object val = param.getValue();
        if (val.getClass().isArray()) {
            serializeArrayValue(writer, param);
        } else {
            serializeBasicValue(writer, param);
        }
    }

    /*
     * Serialize single basic type or Pojo.
     */
    private void serializeBasicValue(JsonWriter writer, PreparedParameter param) throws IOException {
        assert(param.getValue() != null);
        
        Class<?> type = param.getType();
        if (BASIC_TYPES.contains(type)) {
            serializeSingleBasicValue(writer, param.getValue(), param.getType());
        } else {
            assert(Pojo.class.isAssignableFrom(param.getType()));
            serializeSinglePojo(writer, param);
        }
    }

    private void serializeSinglePojo(JsonWriter writer, PreparedParameter param) throws IOException {
        assert(param.getValue() != null);
        
        serializeSinglePojoImpl(writer, param.getValue(), param.getType());
    }
    
    private void serializeSinglePojoImpl(JsonWriter writer, Object value, Class<?> type) throws IOException {
        // precondition(s)
        assert(Pojo.class.isAssignableFrom(type));
        assert(value instanceof Pojo);
        assert(value != null && !value.getClass().isArray());
        
        TypeAdapter<Pojo> pojoTypeAdapter = getTypeAdapter(Pojo.class);
        pojoTypeAdapter.write(writer, (Pojo)value);
    }

    /*
     * Serialize an array of primitives or Pojos
     */
    private void serializeArrayValue(JsonWriter writer,
            PreparedParameter param) throws IOException {
        Class<?> valType = param.getValue().getClass();
        Class<?> typeType = param.getType();
        // Special case pojo list types: the value class is of array type for
        // them, but the type is the component type.
        if (valType.isArray() && !typeType.isArray()) {
            assert(Pojo.class.isAssignableFrom(typeType));
            serializePojoList(writer, param);
        } else {
            serializeBasicList(writer, param);
        }
    }

    /*
     * Serialize a list of known primitives: boolean, int, long, double, String
     */
    private void serializeBasicList(JsonWriter writer,
            PreparedParameter param) throws IOException {
        // preconditions
        assert(param.getValue() != null);
        assert(param.getValue().getClass().isArray());
        assert(param.getType().isArray());
        
        writer.beginArray();
        int length = Array.getLength(param.getValue());
        for (int i = 0; i < length; i++) {
            Object elemVal = Array.get(param.getValue(), i);
            Class<?> elemType = param.getType().getComponentType();
            serializeSingleBasicValue(writer, elemVal, elemType);
        }
        writer.endArray();
    }

    private void serializeSingleBasicValue(JsonWriter writer, Object object, Class<?> type) throws IOException {
        assert(!type.isArray());
        if (object == null) {
            // must have been a String type
            // as other primitive types won't allow null
            writer.nullValue();
            return;
        }
        assert(!object.getClass().isArray());
        assert(BASIC_TYPES.contains(type));
        if (type.isPrimitive()) {
            if (type == int.class) {
                Integer intVal = (Integer)object;
                writer.value(intVal);
            } else if (type == long.class) {
                writer.value((long)object);
            } else if (type == double.class) {
                writer.value((double)object);
            } else if (type == boolean.class) {
                writer.value((boolean)object);
            }
        } else {
            assert(object instanceof String);
            writer.value((String)object);
        }
    }

    /*
     *  Serialized an array of Pojo and only an array of Pojo
     */
    private void serializePojoList(JsonWriter writer, PreparedParameter param) throws IOException {
        // preconditions
        assert(param.getValue() != null);
        assert(Pojo.class.isAssignableFrom(param.getType()));
        assert(param.getValue().getClass().isArray());
        
        int length = Array.getLength(param.getValue());
        writer.beginArray();
        for (int i = 0; i < length; i++) {
            Object value = Array.get(param.getValue(), i);
            serializeSinglePojoImpl(writer, value, param.getType());
        }
        writer.endArray();
    }
    
    private <T> TypeAdapter<T> getTypeAdapter(Class<T> typeClass) {
        return gson.getAdapter(typeClass);
    }
    
}

