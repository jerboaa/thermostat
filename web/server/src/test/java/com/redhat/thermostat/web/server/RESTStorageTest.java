/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.web.server;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.model.Pojo;
import com.redhat.thermostat.common.storage.Category;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Query;
import com.redhat.thermostat.common.storage.Query.Criteria;
import com.redhat.thermostat.common.storage.Storage;
import com.redhat.thermostat.web.client.RESTStorage;
import com.redhat.thermostat.web.common.StorageWrapper;

public class RESTStorageTest {

    public static class TestClass implements Pojo {
        private String key1;
        private int key2;
        public String getKey1() {
            return key1;
        }
        public void setKey1(String key1) {
            this.key1 = key1;
        }
        public int getKey2() {
            return key2;
        }
        public void setKey2(int key2) {
            this.key2 = key2;
        }
    }

    private Server server;

    private Storage mockStorage;

    @Before
    public void setUp() throws Exception {
        mockStorage = mock(Storage.class);
        StorageWrapper.setStorage(mockStorage);

        server = new Server(8080);
        server.setHandler(new WebAppContext("src/main/webapp", "/"));
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
        server.join();
    }

    @Test
    public void testFind() {
        // Configure mock storage.
        TestClass expected = new TestClass();
        expected.setKey1("fluff");
        expected.setKey2(42);
        when(mockStorage.findPojo(any(Query.class), same(TestClass.class))).thenReturn(expected);

        Query mockQuery = QueryTestHelper.createMockQuery();
        when(mockStorage.createQuery()).thenReturn(mockQuery);

        RESTStorage restStorage = new RESTStorage();
        restStorage.setEndpoint("http://localhost:8080/storage");
        Query query = restStorage.createQuery();
        Key<String> key1 = new Key<>("key1", true);
        Key<Integer> key2 = new Key<>("key2", false);
        Category category = new Category("test", key1, key2);
        query.from(category).where(key1, Criteria.EQUALS, "fluff");

        TestClass result = restStorage.findPojo(query, TestClass.class);

        assertEquals("fluff", result.getKey1());
        assertEquals(42, result.getKey2());
        verify(mockStorage).createQuery();
        verify(mockStorage).findPojo(any(Query.class), same(TestClass.class));
    }
}
