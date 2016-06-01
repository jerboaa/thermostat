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

package com.redhat.thermostat.storage.populator.internal;

import com.redhat.thermostat.common.cli.CompletionInfo;
import com.redhat.thermostat.common.cli.DependencyServices;
import com.redhat.thermostat.common.cli.DirectoryContentsCompletionFinder;
import com.redhat.thermostat.shared.config.CommonPaths;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class StoragePopulatorConfigFinderTest {

    private DependencyServices dependencyServices;
    private StoragePopulatorConfigFinder finder;
    private DirectoryContentsCompletionFinder directoryFinder;

    @Before
    public void setup() {
        dependencyServices = mock(DependencyServices.class);
        when(dependencyServices.hasService(CommonPaths.class)).thenReturn(true);
        when(dependencyServices.getService(CommonPaths.class)).thenReturn(mock(CommonPaths.class));
        finder = new StoragePopulatorConfigFinder(dependencyServices);
        directoryFinder = mock(DirectoryContentsCompletionFinder.class);
        finder.setDirectoryFinder(directoryFinder);
    }

    @Test
    public void testDelegatesFindCompletions() {
        finder.findCompletions();
        verify(directoryFinder).findCompletions();
    }

    @Test
    public void testReturnsEmptyResultWithoutDelegationIfCommonPathsUnavailable() {
        when(dependencyServices.hasService(CommonPaths.class)).thenReturn(false);
        when(dependencyServices.getService(CommonPaths.class)).thenReturn(null);
        List<CompletionInfo> result = finder.findCompletions();
        verifyZeroInteractions(directoryFinder);
        assertThat(result.size(), is(0));
    }

}
