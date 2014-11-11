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

import static org.junit.Assert.assertTrue;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * 
 * A generic GSON-based performance test for JSON serialization/deserialization.
 *
 * @param <T> The type which should get serialized/deserialized.
 * 
 * @see PreparedParameterJSONPerformanceTest
 */
public abstract class JsonPerformanceTest<T> {
    
    public enum OperationType {
        DESERIALIZATION,
        SERIALIZATION
    }
    
    public enum GsonContext {
        SLOW,
        FASTER
    }
    
    private static final String EXPECTATION_FORMAT = " (>=%s)";
    private static final int ITERATIONS_FOR_CUMMULATIVE_AVG = 10;
    
    private final boolean debug;
    private final String testClass;
    private final Gson oldGson;
    private final Gson newGson;
    
    protected JsonPerformanceTest(boolean debug, String testClass) {
        this.debug = debug;
        this.testClass = testClass;
        this.oldGson = getSlowGson();
        this.newGson = getFasterGson();
    }
    
    /**
     * 
     * @return The base Gson to measure performance against
     */
    protected abstract Gson getSlowGson();

    /**
     * 
     * @return The "improved" Gson for which the improvement over the Gson as returned by {{@link #getSlowGson()}
     *         should be measured.
     */
    protected abstract Gson getFasterGson();
    
    /**
     * The expected speedup used in asserts for the given iterations.
     * 
     * @param type The type of operation: serialization or deserialization.
     * @param iterations
     * @return The expected speed-up.
     */
    protected abstract double getExpectedSpeedup(OperationType type, int iterations);
    
    /**
     * Formats the string to a valid json string by using
     * {@code mutator}.
     * 
     * @param context The Gson context for this mutation.
     * @param mutator A unique number for a performance run.
     * 
     * @return A valid JSON string representing an instance of type T.
     */
    protected abstract String mutateJsonString(GsonContext context, int mutator);
    
    /**
     * Creates a mutated instance of T using mutator
     * {@code mutator}.
     * 
     * @param mutator      A unique number for this performance test run.
     * 
     * @return The mutated instance.
     */
    protected abstract T mutateToBeSerializedInstance(int mutator);
    
    /**
     * 
     * @return The {@link TypeToken} for type T so that it can be properly
     *         deserialized.
     */
    protected abstract TypeToken<T> getDeserializeTypeToken();
    
    /**
     * 
     * @return The number of iterations to run for the cold (not warmed-up)
     *         serialization performance test.
     */
    protected abstract int getColdSerializationIterations();
    
    /**
     * 
     * @return The number of iterations to run for the "warmed-up" serialization
     *         performance test.
     */
    protected abstract int getWarmSerializationIterations();
    
    /**
     * 
     * @return The number of iterations to run for the cold (not warmed-up)
     *         deserialization performance test.
     */
    protected abstract int getColdDeserializationIterations();
    
    /**
     * 
     * @return The number of iterations to run for the "warmed-up" deserialization
     *         performance test.
     */
    protected abstract int getWarmDeserializationIterations();
    
    /**
     * 
     * @return A delta to increase the "faster" gson serialization speed-up so
     *         that basic assertions such as {@code speed-up >= 1} work. This is
     *         useful for the base-case of comparing a gson instance to itself.
     *         Two performance runs of the same gson are expected to be
     *         approximately equally fast. Though, it will depend on the system.
     */
    protected abstract double getSelfSerializationDelta();
    
    /**
     * 
     * @return A delta to increase the "faster" gson deserialization speed-up so
     *         that basic assertions such as {@code speed-up >= 1} work. This is
     *         useful for the base-case of comparing a gson instance to itself.
     *         Two performance runs of the same gson are expected to be
     *         approximately equally fast. Though, it will depend on the system.
     */
    protected abstract double getSelfDeserializationDelta();
    
    @Test
    public void verifySerializationSpeedCold() {
        int iterations = getColdSerializationIterations();
        double actualSpeedup = getAverageSerializationSpeedup(iterations);
        final double expectedSpeedup = getExpectedSpeedup(OperationType.SERIALIZATION, iterations);
        if (debug) {
            System.out.println(testClass + ": actual cold serialization speed-up: " +
                    formatSpeedup(actualSpeedup) + String.format(EXPECTATION_FORMAT, formatSpeedup(expectedSpeedup)));
        }
        assertTrue("Performance regression? Expected a speed-up of > " + expectedSpeedup +". Speed-up was: " + actualSpeedup, actualSpeedup > expectedSpeedup);
    }
    
    @Test
    public void verifySerializationSpeedWarm() {
        int iterations = getWarmSerializationIterations();
        double actualSpeedup = getAverageSerializationSpeedup(iterations);
        final double expectedSpeedup = getExpectedSpeedup(OperationType.SERIALIZATION, iterations);
        if (debug) {
            System.out.println(testClass + ": actual warmed-up serialization speed-up: " + 
                    formatSpeedup(actualSpeedup) + String.format(EXPECTATION_FORMAT, formatSpeedup(expectedSpeedup)));
        }
        assertTrue("Performance regression? Expected a speed-up of > " + expectedSpeedup +". Speed-up was: " + actualSpeedup, actualSpeedup > expectedSpeedup);
    }
    
    @Test
    public void verifyDeserializationSpeedCold() {
        int iterations = getColdDeserializationIterations();
        double actualSpeedup = getAverageDeserializationSpeedup(iterations);
        final double expectedSpeedup = getExpectedSpeedup(OperationType.DESERIALIZATION, iterations);
        if (debug) {
            System.out.println(testClass + ": actual cold deserialization speed-up: " +
                formatSpeedup(actualSpeedup) + String.format(EXPECTATION_FORMAT, formatSpeedup(expectedSpeedup)));
        }
        assertTrue("Performance regression? Expected a speed-up of > " + expectedSpeedup +". Speed-up was: " + actualSpeedup, actualSpeedup > expectedSpeedup);
    }
    
    @Test
    public void verifyDeserializationSpeedWarm() {
        int iterations = getWarmDeserializationIterations();
        double actualSpeedup = getAverageDeserializationSpeedup(iterations);
        final double expectedSpeedup = getExpectedSpeedup(OperationType.DESERIALIZATION, iterations);
        if (debug) {
            System.out.println(testClass + ": actual warmed-up deserialization speed-up: " +
                    formatSpeedup(actualSpeedup) + String.format(EXPECTATION_FORMAT, formatSpeedup(expectedSpeedup)));
        }
        assertTrue("Performance regression? Expected a speed-up of > " + expectedSpeedup +". Speed-up was: " + actualSpeedup, actualSpeedup > expectedSpeedup);
    }
    
    private double getAverageSerializationSpeedup(final int iterations) {
        double sum = 0;
        for (int i = 0; i < ITERATIONS_FOR_CUMMULATIVE_AVG; i++) {
            sum += measureSerializationSpeed(iterations);
        }
        double speedup = sum/ITERATIONS_FOR_CUMMULATIVE_AVG; 
        double delta = getSelfSerializationDelta();
        assertTrue("Performance Regression? Expected a speed-up of >= 1, but was: " + (speedup + delta),
                        (speedup + delta) >= 1);
        return speedup;
    }
    
    private double measureSerializationSpeed(final int iterations) {
        PerfTestResult result = runSerializationPerformanceTest(iterations);
        return result.getSpeedup();
    }
    
    private double getAverageDeserializationSpeedup(final int iterations) {
        double sum = 0;
        for (int i = 0; i < ITERATIONS_FOR_CUMMULATIVE_AVG; i++) {
            sum += measureDeserializationSpeed(iterations);
        }
        double speedup = sum/ITERATIONS_FOR_CUMMULATIVE_AVG; 
        double delta = getSelfDeserializationDelta();
        assertTrue("Performance Regression? Expected a speed-up of >= 1, but was: " + (speedup + delta),
                        (speedup + delta) >= 1);
        return speedup;
    }
    
    private double measureDeserializationSpeed(final int iterations) {
        PerfTestResult result = runDeserializationPerformanceTest(iterations);
        return result.getSpeedup();
    }
    
    private PerfTestResult runSerializationPerformanceTest(final int iterations) {
        List<String> list = new ArrayList<>();
        double oldSum = 0;
        double newSum = 0;
        long start = -1;
        long end = -1;
        Gson gson = null;
        for (int i = 0; i < iterations * 2; i++) {
            T instance = mutateToBeSerializedInstance(i);
            if (i % 2 != 0) {
                gson = newGson;
            } else {
                gson = oldGson;
            }
            start = System.nanoTime();
            String json = gson.toJson(instance);
            end = System.nanoTime();
            if (i % 2 != 0) {
                newSum += (end - start);
            } else {
                oldSum += (end - start);
            }
            // Do something silly just so that the JIT does not optimize-out the
            // toJson() call.
            list.add(json);
        }
        PerfTestResult res = new PerfTestResult();
        res.oldPerf = oldSum/iterations;
        res.newPerf = newSum/iterations;
        return res;
    }
    
    private PerfTestResult runDeserializationPerformanceTest(final int iterations) {
        List<T> list = new ArrayList<>();

        double oldSum = 0;
        double newSum = 0;
        long start = -1;
        long end = -1;
        Gson gson = null;
        String json;
        for (int i = 0; i < iterations * 2; i++) {
            if (i % 2 != 0) {
                json = mutateJsonString(GsonContext.FASTER, i);
                gson = newGson;
            } else {
                json = mutateJsonString(GsonContext.SLOW, i);
                gson = oldGson;
            }
            start = System.nanoTime();
            T instance = gson.fromJson(json, getDeserializeTypeToken().getType());
            end = System.nanoTime();
            if (i % 2 != 0) {
                newSum += (end - start);
            } else {
                oldSum += (end - start);
            }
            // Do something silly just so that the JIT does not optimize-out the
            // fromJson() call.
            list.add(instance);
        }
        PerfTestResult res = new PerfTestResult();
        res.oldPerf = oldSum/iterations;
        res.newPerf = newSum/iterations;
        return res;
    }
    
    private String formatSpeedup(double value) {
        DecimalFormat format = new DecimalFormat("#.##");
        return format.format(value);
    }
    
    private static class PerfTestResult {
        private double oldPerf;
        private double newPerf;
        
        private double getSpeedup() {
            return oldPerf/newPerf;
        }
    }
    
}
