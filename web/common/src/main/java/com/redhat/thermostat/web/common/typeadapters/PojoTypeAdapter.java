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

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Objects;

import org.apache.commons.beanutils.PropertyUtils;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.redhat.thermostat.storage.core.Entity;
import com.redhat.thermostat.storage.core.Persist;
import com.redhat.thermostat.storage.model.Pojo;

/**
 * A generic type adapter for types implementing {@link Pojo}. It uses
 * no special knowledge of the actual implementation types.
 *
 */
class PojoTypeAdapter<T extends Pojo> extends TypeAdapter<Pojo> {
    
    private final Class<T> runtimeType;
    private final Gson gson;
    private final TypeAdapterFactory pojoFactory;
    private SoftReference<HashMap<String, PropertyDescriptor>> propsCache;
    
    PojoTypeAdapter(TypeAdapterFactory factory, Gson gson, Class<T> runtimeType) {
        assert(Pojo.class.isAssignableFrom(runtimeType));
        this.runtimeType = Objects.requireNonNull(runtimeType);
        this.gson = gson;
        this.pojoFactory = factory;
        // cache will be filled once first used.
        this.propsCache = new SoftReference<>(null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void write(JsonWriter out, Pojo value) throws IOException {
        // handle null
        if (value == null) {
            out.nullValue();
            return;
        }
        
        Class<?> cls = value.getClass();
        if (! cls.isAnnotationPresent(Entity.class)) {
            System.err.println("attempt to serialize non-Entity class: " + cls.getName());
            throw new IllegalArgumentException("attempt to serialize non-Entity class: " + cls.getName());
        }
        
        out.beginObject();
        
        PropertyDescriptor[] descs = PropertyUtils.getPropertyDescriptors(value);
        for (PropertyDescriptor desc : descs) {
            Method readMethod = desc.getReadMethod();
            if (readMethod != null && readMethod.isAnnotationPresent(Persist.class)) {
                String name = desc.getName();
                out.name(name);
                try {
                    Object val = PropertyUtils.getProperty(value, name);
                    if (val == null) {
                        out.nullValue();
                    } else {
                        // non-null member case
                        if (Pojo.class.isAssignableFrom(val.getClass())) {
                            // recursive case
                            PojoTypeAdapter<T> pojoMemAdapter= new PojoTypeAdapter<>(pojoFactory, gson, (Class<T>)val.getClass());
                            pojoMemAdapter.write(out, (T)val);
                        } else {
                            // base case: non-pojo type
                            Class<?> valClass = val.getClass();
                            @SuppressWarnings("rawtypes")
                            TypeToken memberTypeToken = TypeToken.get(valClass);
                            @SuppressWarnings({"rawtypes"})
                            TypeAdapter memberTypeAdapter = getNonPojoTypeAdapter(memberTypeToken);
                            memberTypeAdapter.write(out, val);
                        }
                    }
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            } else if (readMethod == null) {
                System.err.println("WARNING: property without read method: " + value.getClass().getName() + "." + desc.getName());
            }
        }
        
        out.endObject();
    }

    @Override
    public Pojo read(JsonReader in) throws IOException {
        // handle null
        JsonToken token = in.peek();
        if (token == JsonToken.NULL) {
            return null;
        }
        
        
        try {
            in.beginObject();
            
            Pojo pojo = runtimeType.newInstance();
            // loop over names in JSON
            processNamesValues(pojo, in);
            
            in.endObject();

            return pojo;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void processNamesValues(Pojo pojo, JsonReader in) throws IOException {
        JsonToken token = in.peek();
        switch(token) {
        case NAME:
            String name = in.nextName();
            processValue(pojo, in, name);
            // recursive case
            processNamesValues(pojo, in);
            break;
        case END_OBJECT:
            // Base case
            return;
        default:
            throw new IllegalStateException("Expected NAME or END_OBJECT. Was: " + token);
        }
    }

    private void processValue(Pojo pojo, JsonReader in, String name) throws IOException {
        try {
            PropertyDescriptor desc = getPropDescriptor(pojo, name);
            if (desc == null) {
                throw new IllegalStateException("Property descriptor null for: " + name);
            }
            Method writeMethod = desc.getWriteMethod();
            Object value = null;
            if (writeMethod != null && writeMethod.isAnnotationPresent(Persist.class)) {
                Class<?> memberType = desc.getPropertyType();
                if (Pojo.class.isAssignableFrom(memberType)) {
                    // recursive case
                    @SuppressWarnings("unchecked") // We've just checked the cast to Class<T> works
                    PojoTypeAdapter<T> memberAdapter = new PojoTypeAdapter<>(pojoFactory, gson, (Class<T>)memberType);
                    value = (Pojo)memberAdapter.read(in);
                } else {
                    // base case: non-pojo type
                    TypeToken<?> memberTypeToken = TypeToken.get(memberType);
                    TypeAdapter<?> memberTypeAdapter = getNonPojoTypeAdapter(memberTypeToken);
                    value = memberTypeAdapter.read(in);
                }
                PropertyUtils.setProperty(pojo, name, value);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            System.err.println("Setter for '" + name + "' not found. The value will be ignored.");
        }
    }
    
    private PropertyDescriptor getPropDescriptor(Pojo pojo, String name) {
        if (propsCache.get() == null) {
            // build cache
            HashMap<String, PropertyDescriptor> propDescCache = new HashMap<>();
            PropertyDescriptor[] descs = PropertyUtils.getPropertyDescriptors(pojo);
            PropertyDescriptor wanted = null;
            for (PropertyDescriptor desc: descs) {
                propDescCache.put(desc.getName(), desc);
                if (desc.getName().equals(name)) {
                    wanted = desc;
                }
            }
            propsCache = new SoftReference<>(propDescCache);
            return wanted;
        } else {
            HashMap<String, PropertyDescriptor> cacheMap = propsCache.get();
            return cacheMap.get(name);
        }
    }

    private <S> TypeAdapter<S> getNonPojoTypeAdapter(TypeToken<S> nonPojoType) {
        return gson.getDelegateAdapter(pojoFactory, nonPojoType);
    }

}
