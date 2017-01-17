/*
 * Copyright 2012-2017 Red Hat, Inc.
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

import java.util.UUID;

import org.junit.experimental.categories.Category;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.testutils.PerformanceTest;
import com.redhat.thermostat.web.common.SharedStateId;
import com.redhat.thermostat.web.common.WebPreparedStatement;
import com.redhat.thermostat.web.common.typeadapters.PojoTypeAdapterFactory;
import com.redhat.thermostat.web.common.typeadapters.WebPreparedStatementTypeAdapterFactory;

/**
 * JUnit categorized performance test. It'll be only run for
 * the perf-tests profile during a full build.
 */
@Category(PerformanceTest.class)
@SuppressWarnings("rawtypes")
public class WebPreparedStatementJSONPerformanceTest extends
        JsonPerformanceTest<WebPreparedStatement> {

    private static final boolean DEBUG = true;
    
    public WebPreparedStatementJSONPerformanceTest() {
        super(DEBUG, WebPreparedStatementJSONPerformanceTest.class.getSimpleName());
    }
    
    @Override
    protected Gson getSlowGson() {
        return new GsonBuilder()
                    .registerTypeHierarchyAdapter(Pojo.class, new LegacyGSONConverter())
                    .registerTypeAdapterFactory(new SharedStateIdTypeAdapterFactory())
                    .registerTypeAdapter(WebPreparedStatement.class, new LegacyWebPreparedStatementSerializer())
                    .create();
    }

    @Override
    protected Gson getFasterGson() {
        return new GsonBuilder()
                    .registerTypeAdapterFactory(new PojoTypeAdapterFactory())
                    .registerTypeAdapterFactory(new SharedStateIdTypeAdapterFactory())
                    .registerTypeAdapterFactory(new WebPreparedStatementTypeAdapterFactory())
                    .create();
    }

    @Override
    protected double getExpectedSpeedup(OperationType type, int iterations) {
        if (type == OperationType.SERIALIZATION && iterations == getColdSerializationIterations()) {
            return 0.95;
        } else if (type == OperationType.SERIALIZATION && iterations == getWarmSerializationIterations()) {
            return 1.5;
        } else if (type == OperationType.DESERIALIZATION && iterations == getColdDeserializationIterations()) {
            return 0.95;
        } else if (type == OperationType.DESERIALIZATION && iterations == getWarmDeserializationIterations()) {
            return 2;
        } else {
            throw new IllegalStateException("Bug");
        }
    }

    @Override
    protected String mutateJsonString(GsonContext ctx, int mutator) {
        return String.format("{\"sid\":{\"sid\":%d,\"stok\":\"" + UUID.randomUUID() + "\"},\"p\":{\"params\":[]}}", mutator);
    }

    @Override
    protected WebPreparedStatement mutateToBeSerializedInstance(int mutator) {
        SharedStateId id = new SharedStateId(mutator, UUID.randomUUID());
        WebPreparedStatement retval = new WebPreparedStatement<>(0, id);
        return retval;
    }

    @Override
    protected TypeToken<WebPreparedStatement> getDeserializeTypeToken() {
        return TypeToken.get(WebPreparedStatement.class);
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
        return 0.4;
    }

    @Override
    protected double getSelfDeserializationDelta() {
        return 0.1;
    }

}
