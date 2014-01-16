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

package com.redhat.thermostat.storage.core;

import java.lang.reflect.Modifier;
import java.util.Objects;

import com.redhat.thermostat.storage.model.Pojo;

/**
 * 
 * Shared class for setting prepared parameters.
 *
 */
public final class PreparedParameters implements PreparedStatementSetter {

    private PreparedParameter[] params;
    
    public PreparedParameters(int numParams) {
        params = new PreparedParameter[numParams];
    }
    
    @SuppressWarnings("unused")
    private PreparedParameters() {
        // nothing. Exists for serialization purposes.
    }
    
    @Override
    public void setLong(int paramIndex, long paramValue) {
        setType(paramIndex, paramValue, long.class);
    }

    @Override
    public void setLongList(int paramIndex, long[] paramValue) {
        setType(paramIndex, paramValue, long[].class);
    }

    @Override
    public void setInt(int paramIndex, int paramValue) {
        setType(paramIndex, paramValue, int.class);
    }

    @Override
    public void setIntList(int paramIndex, int[] paramValue) {
        setType(paramIndex, paramValue, int[].class);
    }

    @Override
    public void setBoolean(int paramIndex, boolean paramValue) {
        setType(paramIndex, paramValue, boolean.class);
    }

    @Override
    public void setBooleanList(int paramIndex, boolean[] paramValue) {
        setType(paramIndex, paramValue, boolean[].class);
    }

    @Override
    public void setString(int paramIndex, String paramValue) {
        setType(paramIndex, paramValue, String.class);
    }

    @Override
    public void setStringList(int paramIndex, String[] paramValue) {
        setType(paramIndex, paramValue, String[].class);
    }

    @Override
    public void setDouble(int paramIndex, double paramValue) {
        setType(paramIndex, paramValue, double.class);
    }

    @Override
    public void setDoubleList(int paramIndex, double[] paramValue) {
        setType(paramIndex, paramValue, double[].class);
    }

    @Override
    public void setPojo(int paramIndex, Pojo paramValue) {
        // null Pojo value would make array and non-array types
        // indistinguishable for serialization
        Objects.requireNonNull(paramValue);
        Class<?> runtimeType = paramValue.getClass();
        performPojoChecks(runtimeType, "Type");
        setType(paramIndex, paramValue, runtimeType);
    }

    @Override
    public void setPojoList(int paramIndex, Pojo[] paramValue) {
        // null Pojo value would make array and non-array types
        // indistinguishable for serialization
        Objects.requireNonNull(paramValue);
        Class<?> componentType = paramValue.getClass().getComponentType();
        performPojoChecks(componentType, "Component type");
        setType(paramIndex, paramValue, componentType);
    }
    
    private void performPojoChecks(Class<?> type, String errorMsgPrefix) {
        if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
            // Due to serealization we only allow concrete instantiable types.
            // Instantiation would fail later in ThermostatGSONConverter for this
            // reason anyway. Let's do this check early.            
            throw new IllegalArgumentException(errorMsgPrefix + "'" +
                        type.getName() + "' not instantiable!");
        }
    }

    private void setType(int paramIndex, Object paramValue, Class<?> paramType) {
        if (paramIndex >= params.length) {
            throw new IllegalArgumentException("Parameter index '" + paramIndex + "' out of range.");
        }
        PreparedParameter param = new PreparedParameter(paramValue, paramType);
        params[paramIndex] = param;
    }
    
    public PreparedParameter[] getParams() {
        return params;
    }
}

