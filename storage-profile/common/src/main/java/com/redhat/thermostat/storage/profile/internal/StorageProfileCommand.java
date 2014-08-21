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

package com.redhat.thermostat.storage.profile.internal;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;

import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.CategoryAdapter;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedParameter;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.auth.CategoryRegistration;
import com.redhat.thermostat.storage.core.auth.DescriptorMetadata;
import com.redhat.thermostat.storage.core.auth.StatementDescriptorRegistration;
import com.redhat.thermostat.storage.model.AggregateCount;

public class StorageProfileCommand implements Command, CategoryRegistration, StatementDescriptorRegistration {

    private static final int DEFAULT_ITERATIONS = 10000;

    private static final Key<Long> KEY_LONG = new Key<>("long");
    private static final Key<String> KEY_STRING = new Key<>("string");

    private static final Category<SimpleData> PROFILE_CATEGORY =
            new Category<>("storage-profile", SimpleData.class,
                            Key.AGENT_ID, KEY_STRING, KEY_LONG);

    private static final Category<AggregateCount> AGGREGATE_CATEGORY;

    private static final String CLEAR_ALL_DATA = "REMOVE " + PROFILE_CATEGORY.getName();

    private static final String QUERY_COUNT_DATA = "QUERY-COUNT " + PROFILE_CATEGORY.getName();

    private static final String INSERT_DATA = ""
            + "ADD " + PROFILE_CATEGORY.getName() + " "
            + "SET '" + Key.AGENT_ID.getName() + "' = ?s ,"
            + "    '" + KEY_STRING.getName() + "' = ?s ,"
            + "    '" + KEY_LONG.getName() + "' = ?l";

    private static final String UPDATE_DATA = ""
            + "UPDATE " + PROFILE_CATEGORY.getName() + " "
            + " SET   '" + KEY_STRING.getName() + "' = ?s"
            + " WHERE '" + KEY_LONG.getName() + "' = ?l";

    private static final String QUERY_ALL_DATA = ""
            + "QUERY " + PROFILE_CATEGORY.getName();

    private Storage storage;

    static {
        CategoryAdapter<SimpleData, AggregateCount> adapter = new CategoryAdapter<>(PROFILE_CATEGORY);
        AGGREGATE_CATEGORY = adapter.getAdapted(AggregateCount.class);
    }

    public void setStorage(Storage storage) {
        this.storage = storage;

        storage.registerCategory(PROFILE_CATEGORY);
        storage.registerCategory(AGGREGATE_CATEGORY);
    }

    public void unsetStorage() {
        this.storage = null;
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        if (this.storage == null) {
            ctx.getConsole().getError().println("No storage available");
            return;
        }

        Console console = ctx.getConsole();

        int iterations = DEFAULT_ITERATIONS;
        if (ctx.getArguments().hasArgument("iterations")) {
            iterations = Integer.valueOf(ctx.getArguments().getArgument("iterations"));
        }
        console.getOutput().println("Running " + iterations + " iterations");

        clearAndVerifyAllData(console);

        measureAdd(console, iterations);

        clearAndVerifyAllData(console);

        measureQueryNoResult(console, iterations);

        clearAndVerifyAllData(console);

        measureQuerySingleItem(console, iterations);

        clearAndVerifyAllData(console);

        measureQueryDistinctItems(console, iterations);

        clearAndVerifyAllData(console);

        measureQueryCount(console, iterations);

        clearAndVerifyAllData(console);

        measureUpdate(console, iterations);

        clearAndVerifyAllData(console);
    }

    private void clearAndVerifyAllData(Console console) throws AssertionError {
        clearAllData(console);
        waitForDataCount(console, 0);
    }

    private void clearAllData(Console console) {
        try {
            StatementDescriptor<SimpleData> desc = new StatementDescriptor<>(PROFILE_CATEGORY, CLEAR_ALL_DATA);
            PreparedStatement<SimpleData> statement = storage.prepareStatement(desc);
            statement.execute();
        } catch (StatementExecutionException | DescriptorParsingException e) {
            console.getError().println("Error clearing data");
            throw new AssertionError(e);
        }
    }

    private long countAllData(Console console) {
        try {
            StatementDescriptor<AggregateCount> desc = new StatementDescriptor<>(AGGREGATE_CATEGORY, QUERY_COUNT_DATA);
            PreparedStatement<AggregateCount> statement = storage.prepareStatement(desc);
            Cursor<AggregateCount> cursor = statement.executeQuery();
            assert cursor.hasNext();
            AggregateCount aggregate = cursor.next();
            long count = aggregate.getCount();
            return count;
        } catch (StatementExecutionException | DescriptorParsingException e) {
            console.getError().println("Error counting data");
            throw new AssertionError(e);
        }
    }

    private void measureAdd(Console console, final int ITERATIONS) throws AssertionError {
        try {
            StatementDescriptor<SimpleData> desc = new StatementDescriptor<>(PROFILE_CATEGORY, INSERT_DATA);
            long start = System.nanoTime();
            for (int i = 0; i < ITERATIONS; i++) {
                PreparedStatement<SimpleData> statement = storage.prepareStatement(desc);
                statement.setString(0, "FOO");
                statement.setString(1, "FOO" + i);
                statement.setLong(2, i);
                statement.execute();
            }
            long end = System.nanoTime();
            console.getOutput().println("ADD (x" + ITERATIONS + ") took " + nanosToMicroSeconds(end-start));
            console.getOutput().println("ADD avg was " + nanosToMicroSeconds(1.0 * (end-start) / ITERATIONS));
        } catch (StatementExecutionException e) {
            console.getError().println("Error ADDing data");
            e.printStackTrace(console.getError());
        } catch (DescriptorParsingException parsingException) {
            throw new AssertionError("The descriptor must be valid", parsingException);
        }
    }

    private void measureQueryNoResult(Console console, final int ITERATIONS) {
        try {
            StatementDescriptor<SimpleData> desc = new StatementDescriptor<>(PROFILE_CATEGORY, QUERY_ALL_DATA);
            long start = System.nanoTime();
            for (int i = 0; i < ITERATIONS; i++) {
                PreparedStatement<SimpleData> statement = storage.prepareStatement(desc);
                Cursor<SimpleData> results = statement.executeQuery();
                while (results.hasNext()) {
                    /* discard = */ results.next();
                }
            }
            long end = System.nanoTime();
            console.getOutput().println("QUERY no-result (x" + ITERATIONS + ") took " + nanosToMicroSeconds(end-start));
            console.getOutput().println("QUERY no-result avg was " + nanosToMicroSeconds(1.0 * (end-start) / ITERATIONS));
        } catch (StatementExecutionException e) {
            console.getError().println("Error QUERYing data");
            e.printStackTrace(console.getError());
        } catch (DescriptorParsingException parsingException) {
            throw new AssertionError("The descriptor must be valid", parsingException);
        }
    }

    private void measureQuerySingleItem(Console console, final int ITERATIONS) {
        try {

            // populate single data item
            StatementDescriptor<SimpleData> addDesc = new StatementDescriptor<>(PROFILE_CATEGORY, INSERT_DATA);
            PreparedStatement<SimpleData> insertStatement = storage.prepareStatement(addDesc);
            insertStatement.setString(0, "FOO");
            insertStatement.setString(1, "BAR");
            insertStatement.setLong(2, 1);
            insertStatement.execute();

            waitForDataCount(console, 1);

            // time query
            StatementDescriptor<SimpleData> desc = new StatementDescriptor<>(PROFILE_CATEGORY, QUERY_ALL_DATA);
            long start = System.nanoTime();
            for (int i = 0; i < ITERATIONS; i++) {
                PreparedStatement<SimpleData> statement = storage.prepareStatement(desc);
                Cursor<SimpleData> results = statement.executeQuery();
                boolean firstResult = true;
                while (results.hasNext()) {
                    /* discard = */ results.next();
                    if (!firstResult) {
                        throw new AssertionError("Unexpected reuslts");
                    }
                    firstResult = false;
                }
            }
            long end = System.nanoTime();
            console.getOutput().println("QUERY single (x" + ITERATIONS + ") took " + nanosToMicroSeconds(end-start));
            console.getOutput().println("QUERY single avg was " + nanosToMicroSeconds(1.0 * (end-start) / ITERATIONS ));
        } catch (StatementExecutionException e) {
            console.getError().println("Error QUERYing data");
            e.printStackTrace(console.getError());
        } catch (DescriptorParsingException parsingException) {
            throw new AssertionError("The descriptor must be valid", parsingException);
        }
    }

    private void measureQueryDistinctItems(Console console, final int ITERATIONS) {
        try {

            // populate data
            StatementDescriptor<SimpleData> addDesc = new StatementDescriptor<>(PROFILE_CATEGORY, INSERT_DATA);
            final int DATA_COUNT = 100;
            for (int i = 0; i < DATA_COUNT; i++) {
                PreparedStatement<SimpleData> statement = storage.prepareStatement(addDesc);
                statement.setString(0, "FOO");
                statement.setString(1, "FOO" + i);
                statement.setLong(2, i);
                statement.execute();
            }

            waitForDataCount(console, DATA_COUNT);

            // time query
            StatementDescriptor<SimpleData> desc = new StatementDescriptor<>(PROFILE_CATEGORY, QUERY_ALL_DATA);
            long start = System.nanoTime();
            for (int i = 0; i < ITERATIONS; i++) {
                PreparedStatement<SimpleData> statement = storage.prepareStatement(desc);
                Cursor<SimpleData> results = statement.executeQuery();
                while (results.hasNext()) {
                    /* discard = */ results.next();
                }
            }
            long end = System.nanoTime();
            console.getOutput().println("QUERY multiple (" + DATA_COUNT + ") (x" + ITERATIONS + ") took " + nanosToMicroSeconds(end-start));
            console.getOutput().println("QUERY multiple avg was " + nanosToMicroSeconds(1.0 * (end-start) / ITERATIONS ));
        } catch (StatementExecutionException e) {
            console.getError().println("Error QUERYing data");
            e.printStackTrace(console.getError());
        } catch (DescriptorParsingException parsingException) {
            throw new AssertionError("The descriptor must be valid", parsingException);
        }
    }

    private void measureQueryCount(Console console, final int ITERATIONS) {
        try {

            // populate data
            StatementDescriptor<SimpleData> addDesc = new StatementDescriptor<>(PROFILE_CATEGORY, INSERT_DATA);
            final int DATA_COUNT = 100;
            for (int i = 0; i < DATA_COUNT; i++) {
                PreparedStatement<SimpleData> statement = storage.prepareStatement(addDesc);
                statement.setString(0, "FOO");
                statement.setString(1, "FOO" + i);
                statement.setLong(2, i);
                statement.execute();
            }

            waitForDataCount(console, DATA_COUNT);

            // measure query
            StatementDescriptor<AggregateCount> desc = new StatementDescriptor<>(AGGREGATE_CATEGORY, QUERY_COUNT_DATA);
            long start = System.nanoTime();
            for (int i = 0; i < ITERATIONS; i++) {
                PreparedStatement<AggregateCount> statement = storage.prepareStatement(desc);
                Cursor<AggregateCount> cursor = statement.executeQuery();
                assert cursor.hasNext();
                AggregateCount aggregate = cursor.next();
                long count = aggregate.getCount();
                assert count == DATA_COUNT;
            }
            long end = System.nanoTime();
            console.getOutput().println("QUERY-COUNT (x" + ITERATIONS + ") took " + nanosToMicroSeconds(end-start));
            console.getOutput().println("QUERY-COUNT avg was " + nanosToMicroSeconds(1.0 * (end-start) / ITERATIONS));
        } catch (StatementExecutionException e) {
            console.getError().println("Error QUERY-COUNTing data");
            e.printStackTrace(console.getError());
        } catch (DescriptorParsingException parsingException) {
            throw new AssertionError("The descriptor must be valid", parsingException);
        }
    }

    private void measureUpdate(Console console, final int ITERATIONS) {
        try {

            // populate data
            StatementDescriptor<SimpleData> addDesc = new StatementDescriptor<>(PROFILE_CATEGORY, INSERT_DATA);
            final int DATA_COUNT = 100;
            for (int i = 0; i < DATA_COUNT; i++) {
                PreparedStatement<SimpleData> statement = storage.prepareStatement(addDesc);
                statement.setString(0, "FOO");
                statement.setString(1, "FOO" + i);
                statement.setLong(2, i);
                statement.execute();
            }

            waitForDataCount(console, DATA_COUNT);

            // measure update
            StatementDescriptor<SimpleData> desc = new StatementDescriptor<>(PROFILE_CATEGORY, UPDATE_DATA);
            long start = System.nanoTime();
            for (int i = 0; i < ITERATIONS; i++) {
                PreparedStatement<SimpleData> statement = storage.prepareStatement(desc);
                statement.setString(0, "FOO" + "10");
                statement.setLong(1, 10000);
                statement.execute();
            }
            long end = System.nanoTime();
            console.getOutput().println("UPDATE (x" + ITERATIONS + ") took " + nanosToMicroSeconds(end-start));
            console.getOutput().println("UPDATE avg was " + nanosToMicroSeconds(1.0 * (end-start) / ITERATIONS));
        } catch (StatementExecutionException e) {
            console.getError().println("Error UPDATEing data");
            e.printStackTrace(console.getError());
        } catch (DescriptorParsingException parsingException) {
            throw new AssertionError("The descriptor must be valid", parsingException);
        }
    }

    /** Convert the nanoseconds to microseconds and return a human-readable string */
    private String nanosToMicroSeconds(double nanos) {
        DecimalFormat format = new DecimalFormat("###.##");
        return format.format(nanos * 1E-3) + " µs";
    }

    private void waitForDataCount(Console console, int count) {
        while (countAllData(console) != count) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                console.getError().print("Error waiting for data in storage to stabilize");
                e.printStackTrace(console.getError());
            }
        }
    }

    @Override
    public boolean isStorageRequired() {
        return true;
    }

    @Override
    public DescriptorMetadata getDescriptorMetadata(String descriptor, PreparedParameter[] params) {
        return new DescriptorMetadata();
    }

    @Override
    public Set<String> getStatementDescriptors() {
        Set<String> result = new HashSet<>();
        result.add(CLEAR_ALL_DATA);
        result.add(QUERY_COUNT_DATA);
        result.add(INSERT_DATA);
        result.add(UPDATE_DATA);
        result.add(QUERY_ALL_DATA);
        return result;
    }

    @Override
    public Set<String> getCategoryNames() {
        Set<String> result = new HashSet<>();
        result.add(PROFILE_CATEGORY.getName());
        return result;
    }

}
