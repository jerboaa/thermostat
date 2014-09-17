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

package com.redhat.thermostat.web.common.typeadapters;

import org.junit.experimental.categories.Category;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.redhat.thermostat.storage.core.PreparedParameters;
import com.redhat.thermostat.testutils.PerformanceTest;
import com.redhat.thermostat.web.common.typeadapters.PojoTypeAdapterFactory;
import com.redhat.thermostat.web.common.typeadapters.PreparedParameterTypeAdapterFactory;
import com.redhat.thermostat.web.common.typeadapters.PreparedParametersTypeAdapterFactory;

/**
 * JUnit categorized performance test. It'll be only run for
 * the perf-tests profile.
 */
@Category(PerformanceTest.class)
public class PreparedParametersJSONPerformanceTest extends
        JsonPerformanceTest<PreparedParameters> {
    
    private static final String OLD_JSON_FORMAT = "{\"params\":[]}";
    private static final String NEW_JSON_FORMAT = "[]";
    private static final boolean DEBUG = true;

    public PreparedParametersJSONPerformanceTest() {
        super(DEBUG, PreparedParametersJSONPerformanceTest.class.getSimpleName());
    }

    @Override
    protected Gson getSlowGson() {
        return new GsonBuilder()
                     .registerTypeAdapterFactory(new PojoTypeAdapterFactory())
                     .registerTypeAdapterFactory(new PreparedParameterTypeAdapterFactory())
                     .create();
    }

    @Override
    protected Gson getFasterGson() {
        return new GsonBuilder()
                    .registerTypeAdapterFactory(new PojoTypeAdapterFactory())
                    .registerTypeAdapterFactory(new PreparedParameterTypeAdapterFactory())
                    .registerTypeAdapterFactory(new PreparedParametersTypeAdapterFactory())
                    .create();
    }

    @Override
    protected double getExpectedSpeedup(OperationType type,
            int iterations) {
        if (type == OperationType.SERIALIZATION && iterations == getColdSerializationIterations()) {
            return 2;
        } else if (type == OperationType.SERIALIZATION && iterations == getWarmSerializationIterations()) {
            // actual should really be approaching 1.2.
            return 0.95;
        } else if (type == OperationType.DESERIALIZATION && iterations == getColdDeserializationIterations()) {
            return 1.5;
        } else if (type == OperationType.DESERIALIZATION && iterations == getWarmDeserializationIterations()) {
            return 0.95;
        } else {
            throw new IllegalStateException("bug");
        }
    }

    @Override
    protected TypeToken<PreparedParameters> getDeserializeTypeToken() {
        return TypeToken.get(PreparedParameters.class);
    }

    @Override
    protected int getColdSerializationIterations() {
        return 1;
    }

    @Override
    protected int getWarmSerializationIterations() {
        return 10000;
    }

    @Override
    protected int getColdDeserializationIterations() {
        return 1;
    }

    @Override
    protected int getWarmDeserializationIterations() {
        return 10000;
    }

    @Override
    protected double getSelfSerializationDelta() {
        return 0.3;
    }

    @Override
    protected double getSelfDeserializationDelta() {
        return 0.3;
    }

    @Override
    protected String mutateJsonString(GsonContext context, int mutator) {
        if (context == GsonContext.FASTER) {
            return NEW_JSON_FORMAT;
        } else if (context == GsonContext.SLOW) {
            return OLD_JSON_FORMAT;
        } else {
            throw new IllegalStateException("bug");
        }
    }

    @Override
    protected PreparedParameters mutateToBeSerializedInstance(int mutator) {
        return new PreparedParameters(0);
    }

}
