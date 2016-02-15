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

package com.redhat.thermostat.setup.command.internal.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.shared.config.CommonPaths;

public class StampFilesTest {
    
    private Path testRoot;
    
    @Before
    public void setup() throws IOException {
        testRoot = TestRootHelper.createTestRootDirectory(getClass().getName());
    }
    
    @After
    public void teardown() throws IOException {
        TestRootHelper.recursivelyRemoveTestRootDirectory(testRoot);
    }

    @Test
    public void testCreateDeleteSetupComplete() throws IOException {
        File setupCompleteFile = getStampFileRef("setup-complete.stamp");
        
        CommonPaths paths = mock(CommonPaths.class);
        when(paths.getUserPersistentDataDirectory()).thenReturn(setupCompleteFile.getParentFile());
        StampFiles stampFiles = new StampFiles(paths);
        
        assertFalse(setupCompleteFile.exists());
        stampFiles.createSetupCompleteStamp("Temporarily unlocked");
        String fileData = new String(Files.readAllBytes(setupCompleteFile.toPath()));
        assertTrue(setupCompleteFile.exists());
        assertEquals("Temporarily unlocked", fileData);
        stampFiles.createSetupCompleteStamp("Other content");
        fileData = new String(Files.readAllBytes(setupCompleteFile.toPath()));
        assertTrue(setupCompleteFile.exists());
        assertEquals("Other content", fileData);
        
        stampFiles.deleteSetupCompleteStamp();
        assertFalse(setupCompleteFile.exists());
    }
    
    @Test
    public void testCreateDeleteMongodbUserStamp() throws IOException {
        File mongodbUserDoneFile = getStampFileRef("mongodb-user-done.stamp");
        
        CommonPaths paths = mock(CommonPaths.class);
        when(paths.getUserPersistentDataDirectory()).thenReturn(mongodbUserDoneFile.getParentFile());
        StampFiles stampFiles = new StampFiles(paths);
        
        assertFalse(mongodbUserDoneFile.exists());
        stampFiles.createMongodbUserStamp();
        assertTrue(mongodbUserDoneFile.exists());
        
        stampFiles.deleteMongodbUserStamp();
        assertFalse(mongodbUserDoneFile.exists());
    }
    
    @Test
    public void deleteNonExistingMongodbCompleteStamp() throws IOException {
        File mongodbUserDoneFile = getStampFileRef("mongodb-user-done.stamp");
        CommonPaths paths = mock(CommonPaths.class);
        when(paths.getUserPersistentDataDirectory()).thenReturn(mongodbUserDoneFile.getParentFile());
        StampFiles stampFiles = new StampFiles(paths);
        
        assertFalse(mongodbUserDoneFile.exists());
        try {
            stampFiles.deleteMongodbUserStamp();
            // pass
        } catch (Exception e) {
            fail("Did not expect exception for deleting non-existent file.");
        }
        assertFalse(mongodbUserDoneFile.exists());
    }
    
    @Test
    public void deleteNonExistingSetupCompleteStamp() throws IOException {
        File setupCompleteFile = getStampFileRef("setup-complete.stamp");
        CommonPaths paths = mock(CommonPaths.class);
        when(paths.getUserPersistentDataDirectory()).thenReturn(setupCompleteFile.getParentFile());
        StampFiles stampFiles = new StampFiles(paths);
        
        assertFalse(setupCompleteFile.exists());
        try {
            stampFiles.deleteSetupCompleteStamp();
            // pass
        } catch (Exception e) {
            fail("Did not expect exception for deleting non-existent file.");
        }
        assertFalse(setupCompleteFile.exists());
    }
    
    private File getStampFileRef(String name) throws IOException {
        Path thermostatUserHome = testRoot.resolve("user");
        Path thermostatUserData = thermostatUserHome.resolve("data");
        File file = new File(thermostatUserData.toFile(), name);
        Files.createDirectories(thermostatUserData);
        return file;
    }
}
