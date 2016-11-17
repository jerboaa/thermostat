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

package com.redhat.thermostat.itest;

import com.redhat.thermostat.notes.common.HostNote;
import com.redhat.thermostat.notes.common.HostNoteDAO;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.config.SSLConfiguration;
import com.redhat.thermostat.shared.config.internal.SSLConfigurationImpl;
import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.BackingStorage;
import com.redhat.thermostat.storage.core.Connection;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.StorageCredentials;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.mongodb.internal.MongoStorage;
import com.redhat.thermostat.storage.query.Expression;
import com.redhat.thermostat.storage.query.ExpressionFactory;
import expectj.Spawn;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NotesCommandsTest extends IntegrationTest {

    private static final String USERNAME = "foo";
    private static final String PASSWORD = "bar";

    @BeforeClass
    public static void setUpOnce() throws Exception {
        setupIntegrationTest(NotesCommandsTest.class);

        createFakeSetupCompleteFile();

        addUserToStorage(USERNAME, PASSWORD);
        createAgentAuthFile(USERNAME, PASSWORD);

        startStorage();

        addAgentToStorage();
    }

    @AfterClass
    public static void tearDownOnce() throws Exception {
        stopStorage();
    }

    private static void addAgentToStorage() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Connection.ConnectionListener listener = new CountdownConnectionListener(Connection.ConnectionStatus.CONNECTED, latch);
        BackingStorage mongoStorage = getAndConnectStorage(listener);
        latch.await();
        mongoStorage.getConnection().removeListener(listener);
        mongoStorage.registerCategory(AgentInfoDAO.CATEGORY);

        Add<AgentInformation> add = mongoStorage.createAdd(AgentInfoDAO.CATEGORY);
        AgentInformation pojo = new AgentInformation();
        pojo.setAgentId("foo-agentid");
        pojo.setConfigListenAddress("127.0.0.1");
        pojo.setAlive(true);
        pojo.setStartTime(100L);
        pojo.setStopTime(0L);
        addAgentInfo(add, pojo);
        mongoStorage.getConnection().disconnect();
    }

    private static void addAgentInfo(Add<AgentInformation> add, AgentInformation pojo) {
        add.set(Key.AGENT_ID.getName(), pojo.getAgentId());
        add.set(AgentInfoDAO.ALIVE_KEY.getName(), pojo.isAlive());
        add.set(AgentInfoDAO.START_TIME_KEY.getName(), pojo.getStartTime());
        add.set(AgentInfoDAO.STOP_TIME_KEY.getName(), pojo.getStopTime());
        add.set(AgentInfoDAO.CONFIG_LISTEN_ADDRESS.getName(), pojo.getConfigListenAddress());
        add.apply();
    }

    @Test
    public void testAddNoteWithStandardOptions() throws Exception {
        doAddNoteTest(Arrays.asList("-c", "this is the note content"), "this is the note content");
    }

    @Test
    public void testAddNoteWithStrangeOptions() throws Exception {
        doAddNoteTest(Arrays.asList("some", "unquoted", "--args"), null);
    }

    @Test
    public void testAddNoteWithQuotedFakeArgAndContentOption() throws Exception {
        doAddNoteTest(Arrays.asList("-c", "\"--anotherFake\""), "--anotherFake");
    }

    /**
     * Expects "could not parse options" exception if expectedResult is null
     */
    private void doAddNoteTest(List<String> contentArgs, String expectedResult) throws Exception {
        List<String> args = new ArrayList<>();
        args.add("-a");
        args.add("foo-agentid");
        args.add("add");
        args.addAll(contentArgs);
        Spawn cmd = commandAgainstMongo("notes", args.toArray(new String[args.size()]));
        handleAuthPrompt(cmd, "mongodb://127.0.0.1:27518", USERNAME, PASSWORD);
        cmd.expectClose();

        assertCommandIsFound(cmd);
        assertNoExceptions(cmd);
        if (expectedResult == null) {
            assertCouldNotParseUnrecognizedOptions(cmd);
            return;
        }

        CountDownLatch latch2 = new CountDownLatch(1);
        Connection.ConnectionListener listener2 = new CountdownConnectionListener(Connection.ConnectionStatus.CONNECTED, latch2);
        BackingStorage mongoStorage2 = getAndConnectStorage(listener2);
        latch2.await();
        mongoStorage2.getConnection().removeListener(listener2);
        mongoStorage2.registerCategory(HostNoteDAO.hostNotesCategory);

        Query<HostNote> query = mongoStorage2.createQuery(HostNoteDAO.hostNotesCategory);
        Expression expr = new ExpressionFactory().equalTo(HostNoteDAO.KEY_CONTENT, expectedResult);
        query.where(expr);
        Cursor<HostNote> cursor = query.execute();
        assertThat("Expected storage to have note with content: " + expectedResult, cursor.hasNext(), is(true));
        HostNote note = cursor.next();
        assertThat(note.getContent(), is(expectedResult));

        mongoStorage2.getConnection().disconnect();
    }

    private static void assertCouldNotParseUnrecognizedOptions(Spawn spawn) {
        assertCouldNotParseUnrecognizedOptions(spawn.getCurrentStandardOutContents(), spawn.getCurrentStandardErrContents());
    }

    private static void assertCouldNotParseUnrecognizedOptions(String stdout, String stderr) {
        String message = "Could not parse options: Unrecognized option:";
        boolean outContainsMessage = stdout.contains(message);
        boolean errContainsMessage = stderr.contains(message);
        assertTrue("stdout or stderr should have contained \"" + message + "\"",
                outContainsMessage || errContainsMessage);
    }

    private static BackingStorage getAndConnectStorage(Connection.ConnectionListener listener) {
        final String url = "mongodb://127.0.0.1:27518";
        StorageCredentials creds = new StorageCredentials() {
            @Override
            public String getUsername() {
                return USERNAME;
            }

            @Override
            public char[] getPassword() {
                return PASSWORD.toCharArray();
            }
        };
        CommonPaths paths = mock(CommonPaths.class);
        File tmpFile = new File("/tmp");
        when(paths.getUserConfigurationDirectory()).thenReturn(tmpFile);

        SSLConfiguration sslConf = new SSLConfigurationImpl(paths);
        BackingStorage storage = new MongoStorage(url, creds, sslConf);
        if (listener != null) {
            storage.getConnection().addListener(listener);
        }
        storage.getConnection().connect();
        return storage;
    }

    @Test
    public void testFailsForUnknownSubcommand() throws Exception {
        Spawn cmd = runSubcommand("lsit", "-a", "foo-agentid"); // intentional typo: lsit, rather than list

        assertCommandIsFound(cmd);
        assertNoExceptions(cmd);
        assertThat(cmd.getCurrentStandardErrContents(), containsString("Invalid subcommand: lsit"));
    }

    @Test
    public void testAddNoteSubcommandRegistered() throws Exception {
        Spawn cmd = runSubcommand("add");

        assertCommandIsFound(cmd);
        assertNoExceptions(cmd);
        assertThat(cmd.getCurrentStandardOutContents(), containsString("Missing required option: -c"));
    }

    @Test
    public void testDeleteNoteSubcommandRegistered() throws Exception {
        Spawn cmd = runSubcommand("delete");

        assertCommandIsFound(cmd);
        assertNoExceptions(cmd);
        assertThat(cmd.getCurrentStandardOutContents(), containsString("Missing required option: -n"));
    }

    @Test
    public void testListNotesSubcommandRegistered() throws Exception {
        Spawn cmd = runSubcommand("list");

        assertCommandIsFound(cmd);
        assertNoExceptions(cmd);
        assertThat(cmd.getCurrentStandardErrContents(), containsString("A Host or VM ID must be provided"));
    }

    @Test
    public void testUpdateNoteSubcommandRegistered() throws Exception {
        Spawn cmd = runSubcommand("update");

        assertCommandIsFound(cmd);
        assertNoExceptions(cmd);
        assertThat(cmd.getCurrentStandardOutContents(), containsString("Missing required options: -c, -n"));
    }

    private static Spawn runSubcommand(String subcommand, String... extraArgs) throws Exception {
        List<String> args = new ArrayList<>();
        args.add(subcommand);
        args.addAll(Arrays.asList(extraArgs));
        Spawn cmd = commandAgainstMongo("notes", args.toArray(new String[args.size()]));
        handleAuthPrompt(cmd, "mongodb://127.0.0.1:27518", USERNAME, PASSWORD);
        cmd.expectClose();
        return cmd;
    }

    private static Spawn commandAgainstMongo(String cmd, String... args) throws IOException {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("args must be an array with something");
        }
        List<String> completeArgs = new ArrayList<>();
        completeArgs.add(cmd);
        completeArgs.add("-d");
        completeArgs.add("mongodb://127.0.0.1:27518");
        completeArgs.addAll(Arrays.asList(args));
        return spawnThermostat(true, completeArgs.toArray(new String[0]));
    }

    private static class CountdownConnectionListener implements Connection.ConnectionListener {

        private final Connection.ConnectionStatus target;
        private final CountDownLatch latch;

        private CountdownConnectionListener(Connection.ConnectionStatus target, CountDownLatch latch) {
            this.target = target;
            this.latch = latch;
        }

        @Override
        public void changed(Connection.ConnectionStatus newStatus) {
            assertEquals(target, newStatus);
            latch.countDown();
        }
    }

}
