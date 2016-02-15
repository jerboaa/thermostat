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

import org.junit.experimental.categories.Category;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.redhat.thermostat.storage.core.PreparedParameter;
import com.redhat.thermostat.testutils.PerformanceTest;
import com.redhat.thermostat.web.common.typeadapters.PojoTypeAdapterFactory;
import com.redhat.thermostat.web.common.typeadapters.PreparedParameterTypeAdapterFactory;

/**
 * JUnit categorized performance test. It'll be only run for
 * the perf-tests profile during a full build.
 */
@Category(PerformanceTest.class)
public class PreparedParameterJSONPerformanceTest extends
        JsonPerformanceTest<PreparedParameter> {
    
    private static final boolean DEBUG = true;

    public PreparedParameterJSONPerformanceTest() {
        super(DEBUG, PreparedParameterJSONPerformanceTest.class.getSimpleName());
    }

    @Override
    protected Gson getSlowGson() {
        return new GsonBuilder()
                    .registerTypeAdapterFactory(new PojoTypeAdapterFactory())
                    .registerTypeAdapter(PreparedParameter.class, new LegacyPreparedParameterSerializer())
                    .create();
    }

    @Override
    protected Gson getFasterGson() {
        return new GsonBuilder()
            .registerTypeAdapterFactory(new PojoTypeAdapterFactory())
            .registerTypeAdapterFactory(new PreparedParameterTypeAdapterFactory())
            .create();
    }

    @Override
    protected double getExpectedSpeedup(
            OperationType type,
            int iterations) {
        if (type == OperationType.SERIALIZATION && iterations == getColdSerializationIterations()) {
            return 1.7;
        } else if (type == OperationType.SERIALIZATION && iterations == getWarmSerializationIterations()) {
            return 1.3;
        } else if (type == OperationType.DESERIALIZATION && iterations == getColdDeserializationIterations()) {
            return 1;
        } else if (type == OperationType.DESERIALIZATION && iterations == getWarmDeserializationIterations()) {
            return 1;
        } else {
            throw new IllegalStateException("bug");
        }
    }

    @Override
    protected String mutateJsonString(
            GsonContext context,
            int mutator) {
        return String.format("{\"type\":\"int\",\"value\":%d}", mutator);
    }

    @Override
    protected PreparedParameter mutateToBeSerializedInstance(int mutator) {
        PreparedParameter param = new PreparedParameter();
        param.setValue(mutator);
        param.setType(int.class);
        return param;
    }

    @Override
    protected TypeToken<PreparedParameter> getDeserializeTypeToken() {
        return TypeToken.get(PreparedParameter.class);
    }

    @Override
    protected int getColdSerializationIterations() {
        return 1;
    }

    @Override
    protected int getWarmSerializationIterations() {
        return 1000;
    }

    @Override
    protected int getColdDeserializationIterations() {
        return 1;
    }

    @Override
    protected int getWarmDeserializationIterations() {
        return 1000;
    }

    @Override
    protected double getSelfSerializationDelta() {
        return 0.1;
    }

    @Override
    protected double getSelfDeserializationDelta() {
        return 0.1;
    }

}
