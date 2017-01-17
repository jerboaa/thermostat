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

package com.redhat.thermostat.launcher.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.storage.core.DbService;

public class ShellPromptTest {

    ShellPrompt shellPrompt;

    @Before
    public void setup() {
        shellPrompt = new ShellPrompt();
    }

    @Test
    public void testDefaultConnectedPrompt() throws CommandException {
        DbService dbService = mock(DbService.class);
        when(dbService.getConnectionUrl()).thenReturn("http://unused.url.com");

        shellPrompt.storageConnected(dbService);

        String expected = "Thermostat " + ShellPrompt.DEFAULT_CONNECTED_TOKEN +  " > ";
        assertEquals(expected, shellPrompt.getPrompt());
    }

    @Test
    public void testDefaultDisconnectedPrompt() {
        shellPrompt.storageDisconnected();

        String expected = "Thermostat " + ShellPrompt.DEFAULT_DISCONNECTED_TOKEN + " > ";
        assertEquals(expected, shellPrompt.getPrompt());
    }

    @Test
    public void testInvalidConnectionUrl() throws CommandException {
        DbService dbService = mock(DbService.class);
        when(dbService.getConnectionUrl()).thenReturn("invalid-url");

        shellPrompt.storageConnected(dbService);

        String expected = "Thermostat " + ShellPrompt.DEFAULT_CONNECTED_TOKEN + " > ";
        assertEquals(expected, shellPrompt.getPrompt());
    }

    @Test
    public void testCustomPrompt() {
        String customPrompt = "TMS ::> ";
        Map<String, String> customConfig = new HashMap<>();
        customConfig.put("shell-prompt", customPrompt);
        shellPrompt.overridePromptConfig(customConfig);

        assertEquals(customPrompt, shellPrompt.getPrompt());
    }

    @Test
    public void testConnectToken() {
        String customPrompt = "%connect";
        Map<String, String> customConfig = new HashMap<>();
        customConfig.put("shell-prompt", customPrompt);
        shellPrompt.overridePromptConfig(customConfig);

        String expected = ShellPrompt.DEFAULT_DISCONNECTED_TOKEN;

        assertEquals(expected, shellPrompt.getPrompt());
    }

    @Test
    public void testURLToken() {
        String customPrompt = "%url";
        Map<String, String> customConfig = new HashMap<>();
        customConfig.put("shell-prompt", customPrompt);

        shellPrompt.overridePromptConfig(customConfig);

        String url = "http://blob:9/meow";
        DbService dbService = mock(DbService.class);
        when(dbService.getConnectionUrl()).thenReturn(url);

        shellPrompt.storageConnected(dbService);

        assertEquals(url, shellPrompt.getPrompt());
    }

    @Test
    public void testProtocolToken() {
        String customPrompt = "%protocol";
        Map<String, String> customConfig = new HashMap<>();
        customConfig.put("shell-prompt", customPrompt);

        shellPrompt.overridePromptConfig(customConfig);

        String url = "http://blob:9/meow";
        DbService dbService = mock(DbService.class);
        when(dbService.getConnectionUrl()).thenReturn(url);

        shellPrompt.storageConnected(dbService);

        String expected = "http";

        assertEquals(expected, shellPrompt.getPrompt());
    }

    @Test
    public void testHostToken() {
        String customPrompt = "%host";
        Map<String, String> customConfig = new HashMap<>();
        customConfig.put("shell-prompt", customPrompt);

        shellPrompt.overridePromptConfig(customConfig);

        String url = "http://blob:9/meow";
        DbService dbService = mock(DbService.class);
        when(dbService.getConnectionUrl()).thenReturn(url);

        shellPrompt.storageConnected(dbService);

        String expected = "blob";

        assertEquals(expected, shellPrompt.getPrompt());
    }

    @Test
    public void testPortToken() {
        String customPrompt = "%port";
        Map<String, String> customConfig = new HashMap<>();
        customConfig.put("shell-prompt", customPrompt);

        shellPrompt.overridePromptConfig(customConfig);

        String url = "http://blob:9/meow";
        DbService dbService = mock(DbService.class);
        when(dbService.getConnectionUrl()).thenReturn(url);

        shellPrompt.storageConnected(dbService);

        String expected = "9";

        assertEquals(expected, shellPrompt.getPrompt());
    }

    @Test
    public void testUserToken() {
        String expected = "username";

        String customPrompt = "%user";
        Map<String, String> customConfig = new HashMap<>();
        customConfig.put("shell-prompt", customPrompt);

        shellPrompt.overridePromptConfig(customConfig);

        String url = "http://blob:9/meow";
        DbService dbService = mock(DbService.class);
        when(dbService.getConnectionUrl()).thenReturn(url);
        when(dbService.getUserName()).thenReturn(expected);

        shellPrompt.storageConnected(dbService);

        assertEquals(expected, shellPrompt.getPrompt());
    }

    @Test
    public void testConnectString() {
        String connectString = "%url";
        String url = "http://blob:9/meow";

        String customPrompt = "%connect";
        Map<String, String> customConfig = new HashMap<>();
        customConfig.put("shell-prompt", customPrompt);
        customConfig.put("connected-prompt", connectString);

        shellPrompt.overridePromptConfig(customConfig);

        DbService dbService = mock(DbService.class);
        when(dbService.getConnectionUrl()).thenReturn(url);
        shellPrompt.storageConnected(dbService);

        assertEquals(url, shellPrompt.getPrompt());
    }

    @Test
    public void testComplexCustomPrompt() {
        String url = "http://complex.url.com:9/test";
        DbService dbService = mock(DbService.class);
        when(dbService.getConnectionUrl()).thenReturn(url);

        String customPrompt = "TMS %connect ::> ";
        Map<String, String> customConfig = new HashMap<>();
        customConfig.put("shell-prompt", customPrompt);

        String connectPrompt = "[%url %host %port %protocol]";
        customConfig.put("connected-prompt", connectPrompt);

        shellPrompt.overridePromptConfig(customConfig);
        shellPrompt.storageConnected(dbService);

        String expected = "TMS [http://complex.url.com:9/test complex.url.com 9 http] ::> ";
        assertEquals(expected, shellPrompt.getPrompt());
    }

    @Test
    public void testRepeatTokenPrompt() {
        String url = "http://unused.url.com";
        DbService dbService = mock(DbService.class);
        when(dbService.getConnectionUrl()).thenReturn(url);

        String customPrompt = "TMS %connect %connect ::> ";
        Map<String, String> customConfig = new HashMap<>();
        customConfig.put("shell-prompt", customPrompt);

        shellPrompt.overridePromptConfig(customConfig);

        String expected = "TMS - - ::> ";
        assertEquals(expected, shellPrompt.getPrompt());
    }

    @Test
    public void testConnectTokenExpansion() {
        String url = "http://unused.url.com";
        DbService dbService = mock(DbService.class);
        when(dbService.getConnectionUrl()).thenReturn(url);

        String customPrompt = "TMS %connect ::> ";
        Map<String, String> customConfig = new HashMap<>();
        customConfig.put("shell-prompt", customPrompt);

        String connectPrompt = "[CONNECT %connect]";
        customConfig.put("connected-prompt", connectPrompt);

        String disconnectPrompt = "[DISCONNECT %connect]";
        customConfig.put("disconnected-prompt", disconnectPrompt);

        shellPrompt.overridePromptConfig(customConfig);

        String expectedDisconnect = "TMS [DISCONNECT ] ::> ";
        assertEquals(expectedDisconnect, shellPrompt.getPrompt());

        shellPrompt.storageConnected(dbService);

        String expectedConnect = "TMS [CONNECT ] ::> ";
        assertEquals(expectedConnect, shellPrompt.getPrompt());

        shellPrompt.storageDisconnected();

        assertEquals(expectedDisconnect, shellPrompt.getPrompt());
    }
}
