/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.setup.command.internal.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;

import org.junit.Test;

import com.redhat.thermostat.shared.config.CommonPaths;

public class AuthFileWriterTest {

    @Test
    public void canWriteAuthFile() throws IOException {
        CommonPaths paths = mock(CommonPaths.class);
        File mockAgentAuthFile = File.createTempFile("thermostat-test-", getClass().getName());
        try {
            when(paths.getUserAgentAuthConfigFile()).thenReturn(mockAgentAuthFile);
            AuthFileWriter authWriter = new AuthFileWriter(paths, mock(CredentialsFileCreator.class));
            List<String> contents = Files.readAllLines(mockAgentAuthFile.toPath(), Charset.forName("UTF-8"));
            assertEquals(0, contents.size());
            char[] password = new char[] { 't', 'e', 's', 't' };
            authWriter.setCredentials("damian", password);
            authWriter.write();
            contents = Files.readAllLines(mockAgentAuthFile.toPath(), Charset.forName("UTF-8"));
            assertTrue("username and password must be present", contents.size() > 2);
            assertTrue("username=damian expected to be found in agent.auth file", contents.contains("username=damian"));
            assertTrue("password=test expected to be found in agent.auth file", contents.contains("password=test"));
            assertArrayEquals("Expected password to have been cleared.", new char[] { '\0', '\0', '\0', '\0' }, password);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Did not expect failure on commit()");
        }    
        finally {
            Files.delete(mockAgentAuthFile.toPath());
        }
    }
    
    @Test(expected=NullPointerException.class)
    public void notSettingCredsBeforeWriteThrowsNPE() throws IOException {
        AuthFileWriter authWriter = new AuthFileWriter(mock(CommonPaths.class), mock(CredentialsFileCreator.class));
        authWriter.write(); // expected creds to be set first
    }

}
