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

package com.redhat.thermostat.vm.io.client.swing.internal;

import java.awt.BorderLayout;
import java.awt.Component;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.swing.EdtHelper;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.HeaderPanel;
import com.redhat.thermostat.client.swing.components.MultiChartPanel;
import com.redhat.thermostat.client.swing.components.MultiChartPanel.DataGroup;
import com.redhat.thermostat.client.swing.experimental.ComponentVisibilityNotifier;
import com.redhat.thermostat.common.Duration;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.model.DiscreteTimeData;
import com.redhat.thermostat.vm.io.client.core.LocaleResources;
import com.redhat.thermostat.vm.io.client.core.VmIoView;
import com.redhat.thermostat.vm.io.common.VmIoStat;

public class VmIoPanel extends VmIoView implements SwingComponent {

    private static final Logger logger = LoggingUtils.getLogger(VmIoPanel.class);

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final String TAG_CHARACTERS_READ = "charactersRead";
    private static final String TAG_CHARACTERS_WRITTEN = "charactersWritten";
    private static final String TAG_READ_SYSCALLS = "readSyscalls";
    private static final String TAG_WRITE_SYSCALLS = "writeSyscalls";

    private HeaderPanel visiblePanel;

    private MultiChartPanel chartPanel;

    public VmIoPanel() {
        super();

        initializePanel();

        new ComponentVisibilityNotifier().initialize(visiblePanel, notifier);
    }

    @Override
    public Component getUiComponent() {
        return visiblePanel;
    }

    private void initializePanel() {
        visiblePanel = new HeaderPanel();
        visiblePanel.setHeader(translator.localize(LocaleResources.VM_IO_TITLE));

        JPanel mainPanel = new JPanel(new BorderLayout());

        chartPanel = new MultiChartPanel();

        DataGroup charactersGroup = chartPanel.createGroup();
        DataGroup numberGroup = chartPanel.createGroup();

        chartPanel.getRangeAxis(charactersGroup).setAutoRange(true);
        chartPanel.getRangeAxis(charactersGroup).setLabel(translator.localize(LocaleResources.VM_IO_CHART_CHARACTERS_AXIS_LABEL).getContents());
        chartPanel.getRangeAxis(numberGroup).setAutoRange(true);
        chartPanel.getRangeAxis(numberGroup).setLabel(translator.localize(LocaleResources.VM_IO_CHART_SYSCALLS_AXIS_LABEL).getContents());

        chartPanel.addChart(charactersGroup, TAG_CHARACTERS_READ, translator.localize(LocaleResources.VM_IO_CHART_CHARACTERS_READ_LABEL));
        chartPanel.showChart(charactersGroup, TAG_CHARACTERS_READ);
        chartPanel.addChart(charactersGroup, TAG_CHARACTERS_WRITTEN, translator.localize(LocaleResources.VM_IO_CHART_CHARACTERS_WRITTEN_LABEL));
        chartPanel.showChart(charactersGroup, TAG_CHARACTERS_WRITTEN);
        chartPanel.addChart(numberGroup, TAG_READ_SYSCALLS, translator.localize(LocaleResources.VM_IO_CHART_READ_SYSCALLS_LABEL));
        chartPanel.showChart(numberGroup, TAG_READ_SYSCALLS);
        chartPanel.addChart(numberGroup, TAG_WRITE_SYSCALLS, translator.localize(LocaleResources.VM_IO_CHART_WRITE_SYSCALLS_LABEL));
        chartPanel.showChart(numberGroup, TAG_WRITE_SYSCALLS);

        mainPanel.add(chartPanel, BorderLayout.CENTER);

        visiblePanel.setContent(mainPanel);
    }

    @Override
    public Duration getUserDesiredDuration() {
        try {
            return new EdtHelper().callAndWait(new Callable<Duration>() {
                @Override
                public Duration call() throws Exception {
                    return chartPanel.getUserDesiredDuration();
                }
            });
        } catch (InvocationTargetException | InterruptedException e) {
            logger.log(Level.WARNING, "Error executing task on the EDT", e);
            return null;
        }
    }

    @Override
    public void setAvailableDataRange(Range<Long> availableInterval) {
        // FIXME indicate the total data range to the user somehow
    }

    @Override
    public void addData(final List<VmIoStat> data) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                List<DiscreteTimeData<? extends Number>> charactersRead = new ArrayList<>();
                List<DiscreteTimeData<? extends Number>> charactersWritten = new ArrayList<>();
                List<DiscreteTimeData<? extends Number>> readSyscalls = new ArrayList<>();
                List<DiscreteTimeData<? extends Number>> writeSyscalls = new ArrayList<>();

                for (VmIoStat stat: data) {
                    charactersRead.add(new DiscreteTimeData<Long>(stat.getTimeStamp(), stat.getCharactersRead()));
                    charactersWritten.add(new DiscreteTimeData<Long>(stat.getTimeStamp(), stat.getCharactersWritten()));
                    readSyscalls.add(new DiscreteTimeData<Long>(stat.getTimeStamp(), stat.getReadSyscalls()));
                    writeSyscalls.add(new DiscreteTimeData<Long>(stat.getTimeStamp(), stat.getWriteSyscalls()));
                }

                chartPanel.addData(TAG_CHARACTERS_READ, charactersRead);
                chartPanel.addData(TAG_CHARACTERS_WRITTEN, charactersWritten);
                chartPanel.addData(TAG_READ_SYSCALLS, readSyscalls);
                chartPanel.addData(TAG_WRITE_SYSCALLS, writeSyscalls);
            }
        });
    }

    @Override
    public void clearData() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                chartPanel.clearData(TAG_CHARACTERS_READ);
                chartPanel.clearData(TAG_CHARACTERS_WRITTEN);
                chartPanel.clearData(TAG_READ_SYSCALLS);
                chartPanel.clearData(TAG_WRITE_SYSCALLS);
            }
        });
    }
}
