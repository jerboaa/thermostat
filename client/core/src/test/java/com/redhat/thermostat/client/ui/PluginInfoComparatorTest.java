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

package com.redhat.thermostat.client.ui;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.client.core.views.UIPluginInfo;
import com.redhat.thermostat.client.ui.PluginInfo;
import com.redhat.thermostat.client.ui.PluginInfoComparator;
import com.redhat.thermostat.shared.locale.LocalizedString;

public class PluginInfoComparatorTest {

    private List<UIPluginInfo> plugins;
    private int[] orderValues = {240, 200, 200, 120, 100, 0};
    private String[] pluginNames = {"Memory", "NUMA", "GC", "Profiler", "CPU", "Overview"};

    @Before
    public void setUp() {
        createPluginInfos();
    }

    @Test
    public void testPluginOrder() {
        Collections.sort(plugins, new PluginInfoComparator<UIPluginInfo>());
        assertEquals(plugins.get(0).getLocalizedName().getContents(), "Overview");
        assertEquals(plugins.get(1).getLocalizedName().getContents(), "CPU");
        assertEquals(plugins.get(2).getLocalizedName().getContents(), "Profiler");
        assertEquals(plugins.get(3).getOrderValue(), plugins.get(4).getOrderValue());
        assertEquals(plugins.get(3).getLocalizedName().getContents(), "GC");
        assertEquals(plugins.get(4).getLocalizedName().getContents(), "NUMA");
        assertEquals(plugins.get(5).getLocalizedName().getContents(), "Memory");
    }

    private void createPluginInfos() {
        plugins = new ArrayList<>();
        for (int i = 0; i < pluginNames.length; i++) {
            plugins.add(new PluginInfo(new LocalizedString(pluginNames[i]), mock(UIComponent.class), orderValues[i]));
        }
    }

}