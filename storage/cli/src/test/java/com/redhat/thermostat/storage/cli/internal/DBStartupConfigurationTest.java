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

package com.redhat.thermostat.storage.cli.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.storage.cli.internal.DBStartupConfiguration;

public class DBStartupConfigurationTest {
    
    private File dbLogFile;
    private File dbPidFile;
    private File dbPath;
    
    @Before
    public void setUp() {
        dbLogFile = new File("db.log");
        dbPidFile = new File("db.pid");
        dbPath = new File("somepath");
    }
    
    @After
    public void tearDown() {
        dbLogFile = null;
        dbPidFile = null;
        dbPath = null;
    }

    @Test
    public void canGetConfigFromPropertiesFile() throws Exception {
        File dbProps = new File(this.getClass().getResource("/testDbConfig.properties").getFile());
        File canNotBeFoundFile = new File("");
        DBStartupConfiguration dbConfig = new DBStartupConfiguration(dbProps, canNotBeFoundFile, dbPath, dbLogFile, dbPidFile);
        
        assertEquals(dbLogFile.getAbsolutePath(), dbConfig.getLogFile().getAbsolutePath());
        assertEquals(dbPidFile.getAbsolutePath(), dbConfig.getPidFile().getAbsolutePath());
        assertEquals(dbPath.getAbsolutePath(), dbConfig.getDBPath().getAbsolutePath());
        assertEquals("127.0.0.1", dbConfig.getBindIP());
        assertEquals(27518, dbConfig.getPort());
        assertEquals(true, dbConfig.isSslEnabled());
        assertEquals(new File("/path/to/some/pem/file.pem").getAbsolutePath(), dbConfig.getSslPemFile().getAbsolutePath());
        assertEquals("somepassword", dbConfig.getSslKeyPassphrase());
    }
    
    @Test
    public void canGetConfigFromPropertiesFile2() throws Exception {
        File dbProps = new File(this.getClass().getResource("/testDbConfig2.properties").getFile());
        File canNotBeFoundFile = new File("");
        DBStartupConfiguration dbConfig = new DBStartupConfiguration(dbProps, canNotBeFoundFile, dbPath, dbLogFile, dbPidFile);
        
        assertEquals(dbLogFile.getAbsolutePath(), dbConfig.getLogFile().getAbsolutePath());
        assertEquals(dbPidFile.getAbsolutePath(), dbConfig.getPidFile().getAbsolutePath());
        assertEquals(dbPath.getAbsolutePath(), dbConfig.getDBPath().getAbsolutePath());
        assertEquals("127.0.0.1", dbConfig.getBindIP());
        assertEquals(27518, dbConfig.getPort());
        assertEquals(false, dbConfig.isSslEnabled());
        assertNull(dbConfig.getSslPemFile());
        assertNull(dbConfig.getSslKeyPassphrase());
    }
    
    @Test
    public void missingBindThrowsConfigException() throws Exception {
        File dbProps = new File(this.getClass().getResource("/brokenDbConfig.properties").getFile());
        File canNotBeFoundFile = new File("");
        try {
            new DBStartupConfiguration(dbProps, canNotBeFoundFile, dbPath, dbLogFile, dbPidFile);
            fail("BIND was not specified in properties file");
        } catch (InvalidConfigurationException e) {
            assertEquals("BIND property missing", e.getMessage());
        }
    }
    
    @Test
    public void missingPortThrowsConfigException() throws Exception {
        File dbProps = new File(this.getClass().getResource("/brokenDbConfig2.properties").getFile());
        File canNotBeFoundFile = new File("");
        try {
            new DBStartupConfiguration(dbProps, canNotBeFoundFile, dbPath, dbLogFile, dbPidFile);
            fail("PORT was not specified in properties file");
        } catch (InvalidConfigurationException e) {
            assertEquals("PORT property missing", e.getMessage());
        }
    }
}

