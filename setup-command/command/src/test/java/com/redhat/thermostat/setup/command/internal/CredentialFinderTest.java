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

package com.redhat.thermostat.setup.command.internal;

import com.redhat.thermostat.shared.config.CommonPaths;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CredentialFinderTest {
    private File systemConfigDir = mock(File.class);
    private File userConfigDir = mock(File.class);

    private File realFile1;
    private File realFile2;

    private CommonPaths paths;

    @Before
    public void setUp() throws IOException {
        realFile1 = Files.createTempFile("credentialfinder-unit-test", null).toFile();
        realFile1.createNewFile();

        realFile2 = Files.createTempFile("credentialfinder-unit-test", null).toFile();
        realFile2.createNewFile();

        paths = mock(CommonPaths.class);
        when(paths.getSystemConfigurationDirectory()).thenReturn(systemConfigDir);
        when(paths.getUserConfigurationDirectory()).thenReturn(userConfigDir);
    }

    @After
    public void tearDown() {
        realFile1.delete();
        realFile2.delete();
    }

    @Test
    public void verifyFileFromUserHomeIsUsedIfSystemHomeIsNotUsable() throws Exception {
        CredentialFinder finder = new CredentialFinder(paths) {
            @Override
            File getConfigurationFile(File directory, String name) {
                if (directory == systemConfigDir) {
                    return new File("/does-not-exist/really-it-doesn't");
                } else if (directory == userConfigDir) {
                    return realFile1;
                }
                throw new AssertionError("Unknown test case");
            };
        };

        File config = finder.getConfiguration("web.auth");
        assertEquals(realFile1, config);
    }

    @Test
    public void verifyFileFromSystemHomeIsUsedIfBothAreUsable() throws IOException {
        final File systemFile = realFile1;
        final File userFile = realFile2;

        CredentialFinder finder = new CredentialFinder(paths) {
            @Override
            File getConfigurationFile(File directory, String name) {
                if (directory == systemConfigDir) {
                    return systemFile;
                } else if (directory == userConfigDir) {
                    return userFile;
                }
                throw new AssertionError("Unknown test case");
            };
        };

        File config = finder.getConfiguration("web.auth");
        assertEquals(systemFile, config);
    }

    @Test
    public void verifyFileFromSystemHomeIsUsedIfUserHomeNotUsable() throws IOException {
        CredentialFinder finder = new CredentialFinder(paths) {
            @Override
            File getConfigurationFile(File directory, String name) {
                if (directory == systemConfigDir) {
                    return realFile1;
                } else if (directory == userConfigDir) {
                    return new File("/does-not-exist/really-it-doesn't");
                }
                throw new AssertionError("Unknown test case");
            };
        };

        File config = finder.getConfiguration("web.auth");
        assertEquals(realFile1, config);
    }
}
