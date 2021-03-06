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

package com.redhat.thermostat.thread.client.swing.internal;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.ChartPanel;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.thread.client.common.ThreadTableBean;
import com.redhat.thermostat.thread.client.common.chart.ThreadDeatailsPieChart;
import com.redhat.thermostat.thread.client.common.locale.LocaleResources;
import com.redhat.thermostat.thread.client.common.view.ThreadDetailsView;

public class SwingThreadDetailsView extends ThreadDetailsView implements SwingComponent {

    private JPanel details;
    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();

    SwingThreadDetailsView() {
        details = new JPanel();
        details.setLayout(new BorderLayout(0, 0));
        
        JLabel lblNewLabel = new JLabel(t.localize(LocaleResources.THREAD_DETAILS_EMTPY).getContents());
        lblNewLabel.setIcon(new ImageIcon(getEmptyDetailsIcon().getData().array()));
        details.add(lblNewLabel);
    }
    
    @Override
    public Component getUiComponent() {
        return details;
    }

    @Override
    public void setDetails(ThreadTableBean thread) {
        details.removeAll();
        
        ThreadDetailsChart threadChart = new ThreadDetailsChart();
        
        ChartPanel threadSummary = new ChartPanel(new ThreadDeatailsPieChart(thread).createChart());
        threadChart.add(threadSummary);
        
        details.add(threadChart);
        details.repaint();
    }
}

