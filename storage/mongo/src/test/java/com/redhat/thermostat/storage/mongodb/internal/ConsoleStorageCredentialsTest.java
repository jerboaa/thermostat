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

package com.redhat.thermostat.storage.mongodb.internal;

import java.io.IOException;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.redhat.thermostat.common.tools.StorageAuthInfoGetter;

public class ConsoleStorageCredentialsTest {

    /*
     * Every call to getUsername should call the getters getUsername.
     */
    @Test
    public void readUserNameMultiple() throws IOException {
        StorageAuthInfoGetter getter = mock(StorageAuthInfoGetter.class);
        ConsoleStorageCredentials creds = new ConsoleStorageCredentials(getter);
        creds.getUsername();
        creds.getUsername();
        verify(getter, times(2)).getUserName(null);
    }
    
    @Test
    public void readUserNameOnce() throws IOException {
        StorageAuthInfoGetter getter = mock(StorageAuthInfoGetter.class);
        ConsoleStorageCredentials creds = new ConsoleStorageCredentials(getter);
        creds.getUsername();
        verify(getter, times(1)).getUserName(null);
    }
    
    /*
     * Every call to getPassword should call the getters getPassword.
     * This is to limit password life-time in memory.
     */
    @Test
    public void readPasswordMultiple() throws IOException {
        StorageAuthInfoGetter getter = mock(StorageAuthInfoGetter.class);
        ConsoleStorageCredentials creds = new ConsoleStorageCredentials(getter);
        creds.getPassword();
        creds.getPassword();
        verify(getter, times(2)).getPassword(null);
    }
    
    public void readPasswordOnce() throws IOException {
        StorageAuthInfoGetter getter = mock(StorageAuthInfoGetter.class);
        ConsoleStorageCredentials creds = new ConsoleStorageCredentials(getter);
        creds.getPassword();
        verify(getter, times(1)).getPassword(null);
    }
}
