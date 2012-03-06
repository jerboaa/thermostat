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

package com.redhat.thermostat.client;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.redhat.thermostat.client.appctx.ApplicationContext;
import com.redhat.thermostat.client.ui.VmClassStatPanel;
import com.redhat.thermostat.client.ui.VmClassStatView;
import com.redhat.thermostat.common.VmClassStat;
import com.redhat.thermostat.common.dao.VmClassStatDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class VmClassStatController implements AsyncUiFacade {

    private static final Logger logger = LoggingUtils.getLogger(VmClassStatController.class);

    private class UpdateChartDataTimerTask extends TimerTask {

        @Override
        public void run() {
            UpdateChartDataWorker worker = new UpdateChartDataWorker();
            worker.execute();
        }
        
    }

    private class UpdateChartDataWorker extends SwingWorker<List<DiscreteTimeData<Long>>, Void> {

        @Override
        protected List<DiscreteTimeData<Long>> doInBackground() throws Exception {
            List<VmClassStat> latestClassStats = dao.getLatestClassStats();
            List<DiscreteTimeData<Long>> timeData = new ArrayList<>();
            for (VmClassStat stat : latestClassStats) {
                timeData.add(new DiscreteTimeData<Long>(stat.getTimestamp(), stat.getLoadedClasses()));
            }
            
            return timeData;
        }
        
        @Override
        protected void done() {
            try {
                appendCollectorDataToChart(get(), classSeries);
            } catch (ExecutionException ee) {
                logger.throwing("VmClassStatController.UpdateChartDataWorker", "done", ee);
            } catch (InterruptedException ie) {
                // Preserve interrupted flag to let the EDT handle this.
                Thread.currentThread().interrupt();
            }
        }

        private void appendCollectorDataToChart(List<DiscreteTimeData<Long>> collectorData, TimeSeries collectorSeries) {
            if (collectorData.size() > 0) {

                /*
                 * We have lots of new data to add. we do it in 2 steps:
                 * 1. Add everything with notify off.
                 * 2. Notify the chart that there has been a change. It
                 * does all the expensive computations and redraws itself.
                 */

                for (DiscreteTimeData<Long> data : collectorData) {
                    collectorSeries.add(
                            new FixedMillisecond(data.getTimeInMillis()), data.getData(),
                            /* notify = */false);
                }

                collectorSeries.fireSeriesChanged();
            }

        }
    }

    private VmClassStatView classesView;

    // TODO: Use application wide ScheduledExecutorService thread pool.
    private Timer timer;

    private VmClassStatDAO dao;

    private TimeSeries classSeries;
    private TimeSeriesCollection classSeriesCollection;

    public VmClassStatController(VmRef ref) {
        dao = ApplicationContext.getInstance().getDAOFactory().getVmClassStatsDAO(ref);
        classesView = createView();
        classSeries = new TimeSeries("loadedClasses");
        classSeriesCollection = new TimeSeriesCollection(classSeries);
        classesView.setDataSet(classSeriesCollection);
    }

    protected VmClassStatView createView() {
        return new VmClassStatPanel();
    }

    @Override
    public void start() {
        if (timer == null) {
            timer = new Timer();
        }
        TimerTask updateTimerTask = new UpdateChartDataTimerTask();
        timer.scheduleAtFixedRate(updateTimerTask, 0, TimeUnit.SECONDS.toMillis(5));
    }

    @Override
    public void stop() {
        timer.cancel();
        timer = null;
    }

    public Component getComponent() {
        return classesView.getUIComponent();
    }

}
