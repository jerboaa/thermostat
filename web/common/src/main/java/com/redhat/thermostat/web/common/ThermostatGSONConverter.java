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

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.apache.commons.beanutils.PropertyUtils;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.redhat.thermostat.storage.core.Entity;
import com.redhat.thermostat.storage.core.Persist;
import com.redhat.thermostat.storage.model.Pojo;

public class ThermostatGSONConverter implements JsonSerializer<Pojo>, JsonDeserializer<Pojo> {

    @Override
    public Pojo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        @SuppressWarnings("unchecked")
        Class<? extends Pojo> targetType = (Class<Pojo>) typeOfT;
        try {
            Pojo pojo = targetType.newInstance();
            PropertyDescriptor[] descs = PropertyUtils.getPropertyDescriptors(pojo);
            for (PropertyDescriptor desc : descs) {
                Method writeMethod = desc.getWriteMethod();
                if (writeMethod != null && writeMethod.isAnnotationPresent(Persist.class)) {
                    String name = desc.getName();
                    JsonElement child = json.getAsJsonObject().get(name);
                    Object value = context.deserialize(child, desc.getPropertyType());
                    PropertyUtils.setProperty(pojo, name, value);
                }
            }
            return pojo;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JsonElement serialize(Pojo src, Type typeOfSrc, JsonSerializationContext context) {
        Class<?> cls = (Class<?>) typeOfSrc;
        if (! cls.isAnnotationPresent(Entity.class)) {
            System.err.println("attempt to serialize non-Entity class: " + cls.getName());
            throw new IllegalArgumentException("attempt to serialize non-Entity class: " + cls.getName());
        }
        JsonObject obj = new JsonObject();
        PropertyDescriptor[] descs = PropertyUtils.getPropertyDescriptors(src);
        for (PropertyDescriptor desc : descs) {
            Method readMethod = desc.getReadMethod();
            if (readMethod != null && readMethod.isAnnotationPresent(Persist.class)) {
                String name = desc.getName();
                
                try {
                    Object value = PropertyUtils.getProperty(src, name);
                    obj.add(name, context.serialize(value));
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            } else if (readMethod == null) {
                System.err.println("WARNING: property without read method: " + src.getClass().getName() + "::" + desc.getName());
            }
        }
        return obj;
    }


}

