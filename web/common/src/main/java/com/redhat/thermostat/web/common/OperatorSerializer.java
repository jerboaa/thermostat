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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.redhat.thermostat.storage.query.Operator;

public class OperatorSerializer implements JsonDeserializer<Operator>,
        JsonSerializer<Operator> {
    /* The name of the enum constant */
    static final String PROP_CONST = "PROP_CONST";
    /* The Operator implementation fully-qualified class name */
    static final String PROP_CLASS_NAME = "PROP_CLASS_NAME";

    @Override
    public JsonElement serialize(Operator src, Type typeOfSrc,
            JsonSerializationContext context) {
        JsonObject result = new JsonObject();
        // All concrete Operators should be Enums
        if (src instanceof Enum) {
            Enum<?> operator = (Enum<?>) src;
            result.addProperty(PROP_CONST, operator.name());
            result.addProperty(PROP_CLASS_NAME, operator.getDeclaringClass().getCanonicalName());
        }
        else {
            throw new JsonParseException("Concrete Operator must be an enum");
        }
        return result;
    }

    @Override
    public Operator deserialize(JsonElement json, Type typeOfT,
            JsonDeserializationContext context) throws JsonParseException {
        JsonElement jsonClassName = json.getAsJsonObject().get(PROP_CLASS_NAME);
        if (jsonClassName == null) {
            throw new JsonParseException("Class name must be specified for Operator");
        }
        String className = jsonClassName.getAsString();
        Operator result;
        try {
            Class<?> clazz = Class.forName(className);
            result = getEnum(json, clazz);
        } catch (ClassNotFoundException e) {
            throw new JsonParseException("Unable to deserialize Operator", e);
        }
        return result;
    }

    private <T extends Enum<T> & Operator> Operator getEnum(JsonElement json, Class<?> clazz) {
        if (Operator.class.isAssignableFrom(clazz) && clazz.isEnum()) {
            @SuppressWarnings("unchecked") // Checked with above condition
            Class<T> operatorClass = (Class<T>) clazz;
            JsonElement jsonConst = json.getAsJsonObject().get(PROP_CONST);
            String enumConst = jsonConst.getAsString();
            return Enum.valueOf(operatorClass, enumConst);
        }
        else {
            throw new JsonParseException(clazz.getName() + " must be an Enum implementing Operator");
        }
    }

}
