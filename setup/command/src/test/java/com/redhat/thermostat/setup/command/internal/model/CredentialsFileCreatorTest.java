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

package com.redhat.thermostat.setup.command.internal.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CredentialsFileCreatorTest {

    private Path testRoot;
    private File testFile;
    private CredentialsFileCreator creator;
    
    @Before
    public void setup() throws IOException {
        creator = new CredentialsFileCreator();
        testRoot = TestRootHelper.createTestRootDirectory(getClass().getName());
        testFile = new File(testRoot.toFile(), "foobar.auth");
    }
    
    @After
    public void tearDown() throws IOException {
        TestRootHelper.recursivelyRemoveTestRootDirectory(testRoot);
    }
    
    @Test
    public void testCrendentialFileCreatedIfNotExists() throws IOException {
        File agentAuthFile = mock(File.class);
        when(agentAuthFile.exists()).thenReturn(false);
        when(agentAuthFile.toPath()).thenReturn(testFile.toPath());

        creator.create(agentAuthFile);

        assertTrue(testFile.exists());
    }

    @Test
    public void testCrendentialFileNotCreatedIfAlreadyExists() throws IOException {
        File agentAuthFile = mock(File.class);
        when(agentAuthFile.exists()).thenReturn(true);
        when(agentAuthFile.toPath()).thenReturn(testFile.toPath());

        creator.create(agentAuthFile);

        assertFalse(testFile.exists());
    }
}
