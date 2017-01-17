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

package com.redhat.thermostat.common.cli;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class AbstractCompletionFinderTest {

    private DependencyServices dependencyServices;
    private Class<?>[] deps;
    private List<CompletionInfo> infos;
    private AbstractCompletionFinder finder;

    @Before
    @SuppressWarnings("unchecked")
    public void setup() {
        dependencyServices = mock(DependencyServices.class);
        infos = mock(List.class);
        deps = new Class[0];
        finder = new StubCompletionFinder(dependencyServices, deps, infos);
    }

    @Test
    public void testGetService() {
        Class<?> klazz = List.class; // not a great example dependency, but works
        finder.getService(klazz);
        verify(dependencyServices).getService(klazz);
    }

    @Test
    public void testAllDependenciesAvailableReturnsTrueWhenNoDeps() {
        assertTrue(finder.allDependenciesAvailable());
    }

    @Test
    public void testAllDependenciesAvailableReturnsTrueWhenAllAvailable() {
        deps = new Class[]{ List.class, Set.class };
        when(dependencyServices.hasService(List.class)).thenReturn(true);
        when(dependencyServices.hasService(Set.class)).thenReturn(true);
        finder = new StubCompletionFinder(dependencyServices, deps, infos);
        assertTrue(finder.allDependenciesAvailable());
        verify(dependencyServices).hasService(List.class);
        verify(dependencyServices).hasService(Set.class);
        verifyNoMoreInteractions(dependencyServices);
    }

    @Test
    public void testAllDependenciesAvailableReturnsFalseWhenOneUnavailable() {
        deps = new Class[]{ List.class, Set.class };
        when(dependencyServices.hasService(List.class)).thenReturn(true);
        when(dependencyServices.hasService(Set.class)).thenReturn(false);
        finder = new StubCompletionFinder(dependencyServices, deps, infos);
        assertFalse(finder.allDependenciesAvailable());
        verify(dependencyServices).hasService(List.class);
        verify(dependencyServices).hasService(Set.class);
        verifyNoMoreInteractions(dependencyServices);
    }

    private static class StubCompletionFinder extends AbstractCompletionFinder {

        Class<?>[] deps;
        List<CompletionInfo> infos;

        StubCompletionFinder(DependencyServices dependencyServices, Class<?>[] deps, List<CompletionInfo> infos) {
            super(dependencyServices);
            this.deps = deps;
            this.infos = infos;
        }

        @Override
        protected Class<?>[] getRequiredDependencies() {
            return deps;
        }

        @Override
        public List<CompletionInfo> findCompletions() {
            return infos;
        }
    }

}
