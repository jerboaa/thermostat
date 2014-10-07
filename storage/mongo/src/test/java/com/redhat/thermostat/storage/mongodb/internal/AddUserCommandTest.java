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

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.mongodb.DB;
import com.redhat.thermostat.storage.core.QueuedStorage;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.StorageCredentials;

//There is a bug (resolved as wontfix) in powermock which results in
//java.lang.LinkageError if javax.management.* classes aren't ignored by
//Powermock. More here: http://code.google.com/p/powermock/issues/detail?id=277
//SSL tests need this and having that annotation on method level doesn't seem
//to solve the issue.
@PowerMockIgnore( {"javax.management.*"})
@RunWith(PowerMockRunner.class)
@PrepareForTest({ DB.class })
public class AddUserCommandTest {

    /*
     * Verifies that credentials' methods are called in the right order and
     * the password array is filled with zeros after use.
     */
    @Test
    public void verifyAddUser() {
        DB db = PowerMockito.mock(DB.class);
        CountDownLatch latch = new CountDownLatch(1);
        MongoStorage storage = new MongoStorage(db, latch);
        
        StorageCredentials creds = mock(StorageCredentials.class);
        String username = "fooUser";
        char[] password = new char[] { 'f', 'o', 'o' };
        when(creds.getUsername()).thenReturn(username);
        when(creds.getPassword()).thenReturn(password);
        InOrder inOrder = inOrder(creds);
        
        AddUserCommand command = new AddUserCommand(null /* unused */);
        command.addUser(storage, creds);
        
        // password should have been zero-filled
        assertTrue(Arrays.equals(new char[] { '\0', '\0', '\0' }, password));
        // First username, then password should get called.
        inOrder.verify(creds).getUsername();
        inOrder.verify(creds).getPassword();
    }
    
    /*
     * Verifies if the delegate can be retrieved from QueuedStorage since
     * AddUserCommand relies on this. In particular the delegate needs to be
     * a MongoStorage instance.
     */
    @Test
    public void verifyGettingDelegateWorks() {
        DB db = PowerMockito.mock(DB.class);
        CountDownLatch latch = new CountDownLatch(1);
        // Delegate must be mongostorage
        MongoStorage storage = new MongoStorage(db, latch);
        
        QueuedStorage qStorage = new QueuedStorage(storage);
        AddUserCommand command = new AddUserCommand(null /* unused */);
        Storage actual = command.getDelegate(qStorage);
        
        assertSame(storage, actual);
    }
}
