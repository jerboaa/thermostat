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

package com.redhat.thermostat.vm.byteman.client.cli;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;

import com.redhat.thermostat.common.cli.InvalidSubcommandException;
import com.redhat.thermostat.common.cli.SubcommandExpectedException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CliCommandOption;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.FileNameTabCompleter;
import com.redhat.thermostat.common.cli.TabCompleter;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.common.internal.test.TestCommandContextFactory;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.common.utils.StreamUtils;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.vm.byteman.common.BytemanMetric;
import com.redhat.thermostat.vm.byteman.common.VmBytemanDAO;
import com.redhat.thermostat.vm.byteman.common.VmBytemanStatus;
import com.redhat.thermostat.vm.byteman.common.command.BytemanRequest;
import com.redhat.thermostat.vm.byteman.common.command.BytemanRequest.RequestAction;
import com.redhat.thermostat.vm.byteman.common.command.BytemanRequestResponseListener;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class BytemanControlCommandTest {

    private static final String RULE_OPTION = "rules";
    private static final String STATUS_ACTION = "status";
    private static final String SHOW_METRICS_ACTION = "show-metrics";
    private static final String UNLOAD_ACTION = "unload";
    private static final String LOAD_ACTION = "load";
    private static final String SOME_VM_ID = "some-vm-id";
    private static final String SOME_AGENT_ID = "some-agent-id";
    private static final String EMPTY_STRING = "";
    private static final int SOME_LISTEN_PORT = 333;
    private static final InetSocketAddress REQUEST_QUEUE_ADDRESS = mock(InetSocketAddress.class);
    private BytemanControlCommand command;
    private TestCommandContextFactory ctxFactory;
    private static TimeZone defaultTimeZone;
    private static Locale defaultLocale;

    @Before
    public void setup() {
        command = new BytemanControlCommand() {
            @Override
            void waitWithTimeout(CountDownLatch latch) {
                // return immediately
            }
        };
        VmInfoDAO vmInfoDAO = mock(VmInfoDAO.class);
        VmInfo vmInfo = new VmInfo();
        vmInfo.setAgentId(SOME_AGENT_ID);
        vmInfo.setVmId(SOME_VM_ID);
        when(vmInfoDAO.getVmInfo(any(VmId.class))).thenReturn(vmInfo);
        command.bindVmInfoDao(vmInfoDAO);
        AgentInfoDAO agentInfoDAO = mock(AgentInfoDAO.class);
        AgentInformation agentInfo = mock(AgentInformation.class);
        when(agentInfo.isAlive()).thenReturn(true);
        when(agentInfo.getRequestQueueAddress()).thenReturn(REQUEST_QUEUE_ADDRESS);
        when(agentInfoDAO.getAgentInformation(any(AgentId.class))).thenReturn(agentInfo);
        command.bindAgentInfoDao(agentInfoDAO);
        command.bindVmBytemanDao(mock(VmBytemanDAO.class));
        ctxFactory = new TestCommandContextFactory();
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        defaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("EST"));
        defaultLocale = Locale.getDefault(Locale.Category.FORMAT);
        Locale.setDefault(Locale.Category.FORMAT, Locale.CANADA);
    }

    @AfterClass
    public static void tearDownAfterClass() {
        TimeZone.setDefault(defaultTimeZone);
        Locale.setDefault(Locale.Category.FORMAT, defaultLocale);
    }
    
    @Test
    public void testUnknownAction() {
        String unknownAction = "some-action-that-doesn't-exist";
        try {
            Arguments args = getBasicArgsWithAction(unknownAction);
            CommandContext ctx = ctxFactory.createContext(args);
            command.run(ctx);
            fail("Expected failure due to unknown action");
        } catch (CommandException e) {
            String msg = e.getMessage();
            assertThat(msg, containsString(unknownAction));
            assertTrue(msg.startsWith("Invalid subcommand:"));
        }
    }
    
    @Test
    public void testStatusActionNoRule() throws CommandException {
        String rule = null;
        String expectedLoadedRuleMsg = "<no-loaded-rules>";
        VmBytemanStatus status = new VmBytemanStatus();
        status.setVmId(SOME_VM_ID);
        status.setAgentId(SOME_AGENT_ID);
        status.setListenPort(SOME_LISTEN_PORT);
        status.setRule(rule);
        basicStatusActionTest(rule, expectedLoadedRuleMsg, status);
    }
    
    @Test
    public void testStatusActionNotAttached() throws CommandException {
        String rule = null;
        String expectedLoadedRuleMsg = "<no-loaded-rules>";
        basicStatusActionTest(rule, expectedLoadedRuleMsg, null);
    }
    
    private void basicStatusActionTest(String rule, String expectedRuleMsg, VmBytemanStatus status) throws CommandException {
        VmBytemanDAO dao = mock(VmBytemanDAO.class);
        when(dao.findBytemanStatus(eq(new VmId(SOME_VM_ID)))).thenReturn(status);
        Arguments args = getBasicArgsWithAction(STATUS_ACTION);
        CommandContext ctx = ctxFactory.createContext(args);
        command.unbindVmBytemanDao(dao);
        command.bindVmBytemanDao(dao);
        command.run(ctx);
        String stdErr = ctxFactory.getError();
        String stdOut = ctxFactory.getOutput();
        assertEquals(EMPTY_STRING, stdErr);
        assertTrue(stdOut.contains("Byteman status for VM:"));
        String listenPort = "<unset>";
        if (status != null) {
            listenPort = Integer.toString(status.getListenPort());
        }
        assertTrue(stdOut.contains("Byteman agent listen port: " + listenPort));
        assertTrue(stdOut.contains("Loaded rules:"));
        assertTrue(stdOut.contains(expectedRuleMsg));
    }
    
    @Test
    public void testStatusActionWithRule() throws CommandException {
        String rule = "some-rule-string\nsome more rule lines\nfurther more";
        VmBytemanStatus status = new VmBytemanStatus();
        status.setVmId(SOME_VM_ID);
        status.setAgentId(SOME_AGENT_ID);
        status.setListenPort(SOME_LISTEN_PORT);
        status.setRule(rule);
        basicStatusActionTest(rule, rule, status);
    }
    
    @Test
    public void testAgentNotAlive() throws CommandException {
        AgentInfoDAO agentInfoDAO = mock(AgentInfoDAO.class);
        AgentInformation agentInfo = mock(AgentInformation.class);
        when(agentInfo.isAlive()).thenReturn(false);
        when(agentInfoDAO.getAgentInformation(any(AgentId.class))).thenReturn(agentInfo);
        command.unbindAgentInfoDao(agentInfoDAO);
        command.bindAgentInfoDao(agentInfoDAO);
        Arguments args = getBasicArgsWithAction("no-matter");
        CommandContext ctx = ctxFactory.createContext(args);
        try {
            command.run(ctx);
            fail("Command should have thrown exception");
        } catch (CommandException e) {
            String msg = e.getMessage();
            assertEquals("Agent with id " + SOME_AGENT_ID + " is not alive.", msg);
        }
    }
    
    @Test
    public void testShowMetricsActionNoMetrics() throws CommandException {
        String expectedStdOut = "No metrics available for VM " + SOME_VM_ID + ".\n";
        List<BytemanMetric> returnedList = Collections.emptyList();
        doShowMetricsTest(returnedList, expectedStdOut);
    }
    
    @Test
    public void testShowMetricsActionWithMetrics() throws CommandException {
        String metricData1 = "{ \"foo\": \"bar\" }";
        String metricData2 = "{ \"foo2\": -300 }";
        long timestamp = 1_234_567_890_111L;
        String expectedStdOut = "Currently viewing metrics for: all\n" +
            "Available metrics: [foo, foo2]\n" +
            "+----------------------------+---------+------+--------+\n" +
            "| Timestamp                  | Marker  | Name | Value  |\n" +
            "+----------------------------+---------+------+--------+\n" +
            "| 13-Feb-2009 6:31:30 EST PM | marker1 | foo  | bar    |\n" +
            "+----------------------------+---------+------+--------+\n" +
            "| 13-Feb-2009 6:31:30 EST PM | marker2 | foo2 | -300.0 |\n" +
            "+----------------------------+---------+------+--------+\n";
        BytemanMetric metric1 = new BytemanMetric();
        metric1.setMarker("marker1");
        metric1.setData(metricData1);
        metric1.setTimeStamp(timestamp);
        BytemanMetric metric2 = new BytemanMetric();
        metric2.setMarker("marker2");
        metric2.setData(metricData2);
        metric2.setTimeStamp(timestamp);
        List<BytemanMetric> returnedList = Arrays.asList(metric1, metric2);
        doShowMetricsTest(returnedList, expectedStdOut);
    }
    
    @Test
    public void testUnloadAction() throws CommandException {
        VmBytemanDAO dao = mock(VmBytemanDAO.class);
        VmBytemanStatus status = new VmBytemanStatus();
        int listenPort = 333;
        status.setVmId(SOME_VM_ID);
        status.setAgentId(SOME_AGENT_ID);
        status.setListenPort(listenPort);
        status.setRule(null);
        when(dao.findBytemanStatus(eq(new VmId(SOME_VM_ID)))).thenReturn(status);
        Arguments args = getBasicArgsWithAction(UNLOAD_ACTION);
        CommandContext ctx = ctxFactory.createContext(args);
        command.unbindVmBytemanDao(dao);
        command.bindVmBytemanDao(dao);
        RequestQueue rQueue = mock(RequestQueue.class);
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        command.bindRequestQueue(rQueue);
        command.run(ctx);
        verify(rQueue).putRequest(requestCaptor.capture());
        Request submittedRequest = requestCaptor.getValue();
        assertEquals(1, submittedRequest.getListeners().size());
        RequestResponseListener respListener = null; 
        for (RequestResponseListener l: submittedRequest.getListeners()) {
            respListener = l;
            break;
        }
        assertTrue(respListener instanceof BytemanRequestResponseListener);
        String rawAction = submittedRequest.getParameter(BytemanRequest.ACTION_PARAM_NAME);
        RequestAction actualAction = RequestAction.fromIntString(rawAction);
        assertEquals(RequestAction.UNLOAD_RULES, actualAction);
        assertEquals(Integer.toString(listenPort), submittedRequest.getParameter(BytemanRequest.LISTEN_PORT_PARAM_NAME));
        assertEquals(SOME_VM_ID, submittedRequest.getParameter(BytemanRequest.VM_ID_PARAM_NAME));
        assertNull(submittedRequest.getParameter(BytemanRequest.RULE_PARAM_NAME));
        assertSame(REQUEST_QUEUE_ADDRESS, submittedRequest.getTarget());
    }
    
    @Test
    public void testLoadActionNoRuleFile() throws CommandException {
        VmBytemanDAO dao = mock(VmBytemanDAO.class);
        VmBytemanStatus status = new VmBytemanStatus();
        int listenPort = 333;
        status.setVmId(SOME_VM_ID);
        status.setAgentId(SOME_AGENT_ID);
        status.setListenPort(listenPort);
        status.setRule(null);
        when(dao.findBytemanStatus(eq(new VmId(SOME_VM_ID)))).thenReturn(status);
        Arguments args = getBasicArgsWithAction(LOAD_ACTION); // rule file arg missing
        CommandContext ctx = ctxFactory.createContext(args);
        command.unbindVmBytemanDao(dao);
        command.bindVmBytemanDao(dao);
        RequestQueue rQueue = mock(RequestQueue.class);
        command.bindRequestQueue(rQueue);
        try {
            command.run(ctx);
            fail("Expected cmd exception due to missing rule argument");
        } catch (CommandException e) {
            String msg = e.getMessage();
            assertEquals("No rule option specified.", msg);
        }
    }
    
    @Test
    public void testLoadActionRuleFileDoesNotExist() throws CommandException {
        VmBytemanDAO dao = mock(VmBytemanDAO.class);
        VmBytemanStatus status = new VmBytemanStatus();
        int listenPort = 333;
        status.setVmId(SOME_VM_ID);
        status.setAgentId(SOME_AGENT_ID);
        status.setListenPort(listenPort);
        status.setRule(null);
        when(dao.findBytemanStatus(eq(new VmId(SOME_VM_ID)))).thenReturn(status);
        Arguments args = getBasicArgsWithAction(LOAD_ACTION);
        when(args.hasArgument(RULE_OPTION)).thenReturn(true);
        String file = "i-do-not-exist";
        when(args.getArgument(RULE_OPTION)).thenReturn(file);
        CommandContext ctx = ctxFactory.createContext(args);
        command.unbindVmBytemanDao(dao);
        command.bindVmBytemanDao(dao);
        RequestQueue rQueue = mock(RequestQueue.class);
        command.bindRequestQueue(rQueue);
        try {
            command.run(ctx);
            fail("Expected cmd exception due to rule file not existing");
        } catch (CommandException e) {
            String msg = e.getMessage();
            assertEquals("Specified rules file '" + file + "' not found.", msg);
        }
    }
    
    @Test
    public void testLoadActionSuccessAgentAttached() throws CommandException, FileNotFoundException, IOException {
        VmBytemanDAO dao = mock(VmBytemanDAO.class);
        VmBytemanStatus status = new VmBytemanStatus();
        int listenPort = 333;
        status.setVmId(SOME_VM_ID);
        status.setAgentId(SOME_AGENT_ID);
        status.setListenPort(listenPort);
        status.setRule(null);
        when(dao.findBytemanStatus(eq(new VmId(SOME_VM_ID)))).thenReturn(status);
        Arguments args = getBasicArgsWithAction(LOAD_ACTION);
        when(args.hasArgument(RULE_OPTION)).thenReturn(true);
        String file = decodeFilePath(getClass().getResource("/testRule.btm"));
        when(args.getArgument(RULE_OPTION)).thenReturn(file);
        CommandContext ctx = ctxFactory.createContext(args);
        command.unbindVmBytemanDao(dao);
        command.bindVmBytemanDao(dao);
        RequestQueue rQueue = mock(RequestQueue.class);
        command.bindRequestQueue(rQueue);
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        command.run(ctx);
        verify(rQueue).putRequest(requestCaptor.capture());
        Request submittedRequest = requestCaptor.getValue();
        assertEquals(1, submittedRequest.getListeners().size());
        RequestResponseListener respListener = null; 
        for (RequestResponseListener l: submittedRequest.getListeners()) {
            respListener = l;
            break;
        }
        assertTrue(respListener instanceof BytemanRequestResponseListener);
        String rawAction = submittedRequest.getParameter(BytemanRequest.ACTION_PARAM_NAME);
        RequestAction actualAction = RequestAction.fromIntString(rawAction);
        assertEquals(RequestAction.LOAD_RULES, actualAction);
        assertEquals(Integer.toString(listenPort), submittedRequest.getParameter(BytemanRequest.LISTEN_PORT_PARAM_NAME));
        assertEquals(SOME_VM_ID, submittedRequest.getParameter(BytemanRequest.VM_ID_PARAM_NAME));
        String expectedRule = new String(StreamUtils.readAll(new FileInputStream(new File(file))));
        assertEquals(expectedRule, submittedRequest.getParameter(BytemanRequest.RULE_PARAM_NAME));
        assertSame(REQUEST_QUEUE_ADDRESS, submittedRequest.getTarget());
        String out = ctxFactory.getOutput();
        assertEquals("Request submitted successfully.\n", out);
    }
    
    @Test
    public void testLoadActionSuccessAgentNotAttached() throws CommandException, FileNotFoundException, IOException {
        VmBytemanDAO dao = mock(VmBytemanDAO.class);
        // mimic no-agent-attached, by returning a null status
        when(dao.findBytemanStatus(eq(new VmId(SOME_VM_ID)))).thenReturn(null);
        Arguments args = getBasicArgsWithAction(LOAD_ACTION);
        when(args.hasArgument(RULE_OPTION)).thenReturn(true);
        String file = decodeFilePath(getClass().getResource("/testRule.btm"));
        when(args.getArgument(RULE_OPTION)).thenReturn(file);
        CommandContext ctx = ctxFactory.createContext(args);
        command.unbindVmBytemanDao(dao);
        command.bindVmBytemanDao(dao);
        RequestQueue rQueue = mock(RequestQueue.class);
        command.bindRequestQueue(rQueue);
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        command.run(ctx);
        verify(rQueue).putRequest(requestCaptor.capture());
        Request submittedRequest = requestCaptor.getValue();
        assertEquals(1, submittedRequest.getListeners().size());
        RequestResponseListener respListener = null; 
        for (RequestResponseListener l: submittedRequest.getListeners()) {
            respListener = l;
            break;
        }
        assertTrue(respListener instanceof BytemanRequestResponseListener);
        String rawAction = submittedRequest.getParameter(BytemanRequest.ACTION_PARAM_NAME);
        RequestAction actualAction = RequestAction.fromIntString(rawAction);
        assertEquals(RequestAction.LOAD_RULES, actualAction);
        assertEquals(Integer.toString(BytemanRequest.NOT_ATTACHED_PORT), submittedRequest.getParameter(BytemanRequest.LISTEN_PORT_PARAM_NAME));
        assertEquals(SOME_VM_ID, submittedRequest.getParameter(BytemanRequest.VM_ID_PARAM_NAME));
        String expectedRule = new String(StreamUtils.readAll(new FileInputStream(new File(file))));
        assertEquals(expectedRule, submittedRequest.getParameter(BytemanRequest.RULE_PARAM_NAME));
        assertSame(REQUEST_QUEUE_ADDRESS, submittedRequest.getTarget());
        String out = ctxFactory.getOutput();
        assertEquals("Request submitted successfully.\n", out);
    }

    @Test
    public void testProvidesRulesFileCompletion() {
        FileNameTabCompleter fileNameTabCompleter = mock(FileNameTabCompleter.class);
        command.bindFileNameTabCompleter(fileNameTabCompleter);
        Map<CliCommandOption, ? extends TabCompleter> completerMap = command.getOptionCompleters();
        assertThat(completerMap.size(), is(1));
        assertThat(completerMap.keySet(), is(equalTo(Collections.singleton(BytemanControlCommand.RULES_OPTION))));
        assertThat(BytemanControlCommand.RULES_OPTION.getLongOpt(), is("rules"));
        assertThat(BytemanControlCommand.RULES_OPTION.getOpt(), is("r"));
        assertThat(completerMap.get(BytemanControlCommand.RULES_OPTION), is(not(equalTo(null))));
        assertThat(completerMap.get(BytemanControlCommand.RULES_OPTION), is(instanceOf(FileNameTabCompleter.class)));
    }

    @Test
    public void testProvidesNoCompletionsIfFileNameTabCompleterIsUnavailable() {
        FileNameTabCompleter fileNameTabCompleter = mock(FileNameTabCompleter.class);
        command.unbindFileNameTabCompleter(fileNameTabCompleter);
        Map<CliCommandOption, ? extends TabCompleter> completerMap = command.getOptionCompleters();
        assertThat(completerMap.size(), is(0));
    }
    
    @SuppressWarnings("unchecked")
    private void doShowMetricsTest(List<BytemanMetric> metricsToReturn, String stdOutExpected) throws CommandException {
        VmBytemanDAO dao = mock(VmBytemanDAO.class);
        VmBytemanStatus status = new VmBytemanStatus();
        int listenPort = 333;
        status.setVmId(SOME_VM_ID);
        status.setAgentId(SOME_AGENT_ID);
        status.setListenPort(listenPort);
        status.setRule(null);
        when(dao.findBytemanStatus(eq(new VmId(SOME_VM_ID)))).thenReturn(status);
        when(dao.findBytemanMetrics(any(Range.class), any(VmId.class), any(AgentId.class))).thenReturn(metricsToReturn);
        Arguments args = getBasicArgsWithAction(SHOW_METRICS_ACTION);
        CommandContext ctx = ctxFactory.createContext(args);
        command.unbindVmBytemanDao(dao);
        command.bindVmBytemanDao(dao);
        command.run(ctx);
        String stdErr = ctxFactory.getError();
        String stdOut = ctxFactory.getOutput();
        assertEquals(EMPTY_STRING, stdErr);
        assertEquals(stdOutExpected, stdOut);
    }

    private Arguments getBasicArgsWithAction(final String action) throws SubcommandExpectedException, InvalidSubcommandException {
        final Arguments args = mock(Arguments.class);
        when(args.getArgument("vmId")).thenReturn(SOME_VM_ID);
        when(args.getNonOptionArguments()).thenReturn(Collections.singletonList(action));
        when(args.getSubcommand()).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocationOnMock) throws Throwable {
                List<String> options = Arrays.asList(BytemanControlCommand.SHOW_ACTION, BytemanControlCommand.STATUS_ACTION,
                        BytemanControlCommand.UNLOAD_RULE_ACTION, BytemanControlCommand.INJECT_RULE_ACTION);
                if (!options.contains(action)) {
                    throw new InvalidSubcommandException(action);
                }
                return action;
            }
        });
        return args;
    }

    private static String decodeFilePath(URL url) {
        try {
            // Spaces are encoded as %20 in URLs. Use URLDecoder.decode() so
            // as to handle cases like that.
            return URLDecoder.decode(url.getFile(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("UTF-8 not supported, huh?");
        }
    }
}
