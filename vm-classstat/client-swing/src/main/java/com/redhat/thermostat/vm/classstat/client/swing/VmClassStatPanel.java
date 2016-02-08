/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.vm.classstat.client.swing;

import java.awt.Component;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.swing.EdtHelper;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.HeaderPanel;
import com.redhat.thermostat.client.swing.components.MultiChartPanel;
import com.redhat.thermostat.client.swing.components.MultiChartPanel.DataGroup;
import com.redhat.thermostat.client.swing.experimental.ComponentVisibilityNotifier;
import com.redhat.thermostat.common.Duration;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.model.DiscreteTimeData;
import com.redhat.thermostat.vm.classstat.client.core.VmClassStatView;
import com.redhat.thermostat.vm.classstat.client.locale.LocaleResources;

public class VmClassStatPanel extends VmClassStatView implements SwingComponent {

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();

    private final HeaderPanel visiblePanel;

    private final MultiChartPanel multiChartPanel;

    private DataGroup GROUP_NUMBER;
    private DataGroup GROUP_SIZE;

    public VmClassStatPanel() {
        visiblePanel = new HeaderPanel();

        visiblePanel.setHeader(t.localize(LocaleResources.VM_CLASSES_HEADER));

        String xAxisLabel = t.localize(LocaleResources.VM_CLASSES_CHART_TIME_LABEL).getContents();
        String classesAxisLabel = t.localize(LocaleResources.VM_CLASSES_CHART_CLASSES_LABEL).getContents();
        String sizeAxisLabel = t.localize(LocaleResources.VM_CLASSES_CHART_SIZE_LABEL).getContents();

        multiChartPanel = new MultiChartPanel();

        GROUP_NUMBER = multiChartPanel.createGroup();
        GROUP_SIZE = multiChartPanel.createGroup();

        multiChartPanel.setDomainAxisLabel(xAxisLabel);

        multiChartPanel.getRangeAxis(GROUP_NUMBER).setLabel(classesAxisLabel);
        multiChartPanel.getRangeAxis(GROUP_SIZE).setLabel(sizeAxisLabel);

        visiblePanel.setContent(multiChartPanel);

        new ComponentVisibilityNotifier().initialize(visiblePanel, notifier);
    }

    @Override
    public void addClassChart(final Group group, final String tag, final LocalizedString name) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                multiChartPanel.addChart(getGroup(group), tag, name);
                multiChartPanel.showChart(getGroup(group), tag);
            }
        });
    }
    @Override
    public void removeClassChart(final Group group, final String tag) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                multiChartPanel.hideChart(getGroup(group), tag);
                multiChartPanel.removeChart(getGroup(group), tag);
            }
        });
    }

    private MultiChartPanel.DataGroup getGroup(Group group) {
        switch (group) {
        case NUMBER:
            return GROUP_NUMBER;
        case SIZE:
            return GROUP_SIZE;
        default:
            throw new AssertionError("Unknown data group");
        }
    }

    @Override
    public void addClassData(final String tag, final List<DiscreteTimeData<? extends Number>> data) {
        final List<DiscreteTimeData<? extends Number>> copy = new ArrayList<>(data);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                multiChartPanel.addData(tag, copy);
            }
        });
    }

    @Override
    public void clearClassData(final String tag) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                multiChartPanel.clearData(tag);
            }
        });
    }

    @Override
    public Duration getUserDesiredDuration() {
        try {
            return new EdtHelper().callAndWait(new Callable<Duration>() {
                @Override
                public Duration call() throws Exception {
                    return multiChartPanel.getUserDesiredDuration();
                }
            });
        } catch (InvocationTargetException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void setAvailableDataRange(final Range<Long> availableDataRange) {
        // FIXME indicate the total data range to the user somehow
    }

    @Override
    public Component getUiComponent() {
        return visiblePanel;
    }
}

