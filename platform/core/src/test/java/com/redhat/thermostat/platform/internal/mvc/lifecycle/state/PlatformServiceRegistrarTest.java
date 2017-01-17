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

package com.redhat.thermostat.platform.internal.mvc.lifecycle.state;

import com.redhat.thermostat.platform.annotations.PlatformService;
import com.redhat.thermostat.platform.mvc.Model;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 */
public class PlatformServiceRegistrarTest {

    @PlatformService
    private class TestClassEmptyNames extends Model {}

    @PlatformService("TestClass")
    private class TestClassWithValue extends TestClassEmptyNames {}

    @PlatformService(service = { Model.class, TestClassWithServices.class })
    private class TestClassWithServices extends TestClassWithValue {}

    @PlatformService(
            service = { Model.class, TestClassWithServices.class },
            value = "ThisShouldNotAppearInTheList"
    )
    private class TestClassWithServices2 extends Model {}

    private ExecutorService executorService;
    private BundleContext context;

    @Before
    public void setUp() {
        executorService = mock(ExecutorService.class);
        context = mock(BundleContext.class);
    }

    @Test
    public void testAnnotationParserEmpty() throws Exception {
        PlatformServiceRegistrar registrar =
                new PlatformServiceRegistrar(executorService, context);

        List<String> results = registrar.getPlatformServiceIDs(TestClassEmptyNames.class);
        assertEquals(1, results.size());
        assertEquals(TestClassEmptyNames.class.getName(), results.get(0));
    }

    @Test
    public void testAnnotationParserOnlyValue() throws Exception {
        PlatformServiceRegistrar registrar =
                new PlatformServiceRegistrar(executorService, context);

        List<String> results = registrar.getPlatformServiceIDs(TestClassWithValue.class);
        assertEquals(1, results.size());
        assertEquals("TestClass", results.get(0));
    }

    @Test
    public void testAnnotationParserWithServices() throws Exception {
        PlatformServiceRegistrar registrar =
                new PlatformServiceRegistrar(executorService, context);

        List<String> results = registrar.getPlatformServiceIDs(TestClassWithServices.class);
        assertEquals(2, results.size());
        assertTrue(results.contains(TestClassWithServices.class.getName()));
        assertTrue(results.contains(Model.class.getName()));
    }

    @Test
    public void testAnnotationParserWithServices2() throws Exception {
        PlatformServiceRegistrar registrar =
                new PlatformServiceRegistrar(executorService, context);

        List<String> results = registrar.getPlatformServiceIDs(TestClassWithServices.class);
        assertEquals(2, results.size());
        assertTrue(results.contains(TestClassWithServices.class.getName()));
        assertTrue(results.contains(Model.class.getName()));
    }
}
