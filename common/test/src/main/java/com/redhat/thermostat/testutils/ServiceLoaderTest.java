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

package com.redhat.thermostat.testutils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.junit.Test;

public abstract class ServiceLoaderTest<T> {

    public static final int NO_EXTRA_SERVICES = 0;
    public static final int STORAGE_SERVICES = 1;

    private final int extraServices;
    private final Class<T> interfaceClass;
    private final List<Class<? extends T>> implementations;

    /**
     * @param type
     *            the type of service, such as CategoryRegistration
     * @param extraServices
     *            the number of extra services. See the various *_SERVICES
     *            constants.
     * @param implementations
     *            the implementations that are expected to be present. This is
     *            not exclusive; additional services may be present.
     */
    @SafeVarargs
    public ServiceLoaderTest(Class<T> type, int extraServices, Class<? extends T>... implementations) {
        if (implementations == null || implementations.length == 0) {
            throw new IllegalArgumentException("implementations is empty");
        }
        this.extraServices = extraServices;
        this.interfaceClass = type;
        this.implementations = Arrays.asList(implementations);
    }

    /*
     * The web storage end-point uses service loader in order to determine the
     * list of trusted/known categories/statements. This test is to ensure
     * service loading works for this module's regs. E.g. renaming of the impl
     * class without changing
     * META-INF/com.redhat.thermostat.storage.core.auth.CategoryRegistration
     */
    @Test
    public void serviceLoaderCanLoadService() {
        Map<String, Boolean> foundServiceNames = new HashMap<>();
        Class<? extends T> firstClass = implementations.get(0);
        Collection<String> expectedClassNames = new HashSet<>();

        for (Class<? extends T> klass : implementations) {
            expectedClassNames.add(klass.getName());
        }

        ServiceLoader<T> loader = ServiceLoader.load(this.interfaceClass, firstClass.getClassLoader());

        List<T> registrations = new ArrayList<>(1);
        for (T r: loader) {
            foundServiceNames.put(r.getClass().getName(), true);
            registrations.add(r);
        }

        for (String className : expectedClassNames) {
            assertTrue(foundServiceNames.containsKey(className));
        }

        int expectedServicesCount = this.extraServices + implementations.size();
        assertEquals(registrations.size(), expectedServicesCount);
    }

}
