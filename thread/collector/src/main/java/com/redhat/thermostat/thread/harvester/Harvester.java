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

package com.redhat.thermostat.thread.harvester;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.management.MalformedObjectNameException;

import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.thread.collector.impl.ThreadMXInfo;
import com.redhat.thermostat.thread.collector.impl.ThreadMXSummary;
import com.redhat.thermostat.thread.collector.impl.VMThreadMXCapabilities;
import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.utils.management.MXBeanConnection;
import com.redhat.thermostat.utils.management.MXBeanConnector;

@SuppressWarnings("restriction")
class Harvester {
    
    private boolean isConnected;
    private ScheduledExecutorService threadPool;
    private ScheduledFuture<?> harvester;
    
    MXBeanConnector connector;
    
    private MXBeanConnection connection;
    private ThreadMXBean collectorBean;
    private ThreadDao threadDao;
    private String vmId;
    private String agentId;
    
    Harvester(ThreadDao threadDao, ScheduledExecutorService threadPool, String vmId, String agentId) {
        this.connector = new MXBeanConnector(vmId);
        this.threadDao = threadDao;
        this.vmId = vmId;
        this.agentId = agentId;
        this.threadPool = threadPool;
    }
    
    synchronized void start() {
        if (isConnected) return;
              
        if (!connector.isAttached()) {
            try {
                connector.attach();
            } catch (Exception ignore) {
                ignore.printStackTrace();
            }
        }
      
        try {
            connection = connector.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        isConnected = true;
        
        harvester = threadPool.scheduleAtFixedRate(new HarvesterAction(), 0, 1, TimeUnit.SECONDS);
    }
    
    boolean isConnected() {
        return isConnected;
    }
    
    synchronized void stop() {
        if (!isConnected)
            return;
        
        harvester.cancel(false);

        try {
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (connector.isAttached()) {
            try {
                connector.close();
            } catch (Exception ignore) {
                ignore.printStackTrace();
            }
        }

        isConnected = false;
    }
    
    ThreadMXBean getDataCollectorBean(MXBeanConnection connection)
            throws MalformedObjectNameException
    {
        ThreadMXBean bean = null;
        try {
            bean = connection.createProxy(ManagementFactory.THREAD_MXBEAN_NAME,
                                          com.sun.management.ThreadMXBean.class);
        } catch (MalformedObjectNameException ignore) {}

        if (bean == null) {
            bean = connection.createProxy(ManagementFactory.THREAD_MXBEAN_NAME,
                                          ThreadMXBean.class);
        }
        return bean;
    }
    
    synchronized void harvestData() {
      try {
          long timestamp = System.currentTimeMillis();
          
          ThreadMXSummary summary = new ThreadMXSummary();
          
          collectorBean = getDataCollectorBean(connection);
          
          summary.setCurrentLiveThreads(collectorBean.getThreadCount());
          summary.setDaemonThreads(collectorBean.getDaemonThreadCount());
          summary.setTimestamp(timestamp);
          
          threadDao.saveSummary(vmId, agentId, summary);
          
          long [] ids = collectorBean.getAllThreadIds();
          long[] allocatedBytes = null;
          
          // now the details for the threads
          if (collectorBean instanceof com.sun.management.ThreadMXBean) {
              com.sun.management.ThreadMXBean sunBean = (com.sun.management.ThreadMXBean) collectorBean;
              boolean wasEnabled = false;
              if (sunBean.isThreadAllocatedMemorySupported()) {
                  wasEnabled = sunBean.isThreadAllocatedMemoryEnabled();
                  sunBean.setThreadAllocatedMemoryEnabled(true);
                  allocatedBytes = sunBean.getThreadAllocatedBytes(ids);
                  sunBean.setThreadAllocatedMemoryEnabled(wasEnabled);
              }
          }

          ThreadInfo[] threadInfos = collectorBean.getThreadInfo(ids, true, true);
          
          for (int i = 0; i < ids.length; i++) {
              ThreadMXInfo info = new ThreadMXInfo();
              ThreadInfo beanInfo = threadInfos[i];

              info.setTimeStamp(timestamp);

              info.setName(beanInfo.getThreadName());
              info.setID(beanInfo.getThreadId());
              info.setState(beanInfo.getThreadState());
              info.setStackTrace(beanInfo.getStackTrace());

              info.setCPUTime(collectorBean.getThreadCpuTime(info.getThreadID()));
              info.setUserTime(collectorBean.getThreadUserTime(info.getThreadID()));
              
              info.setBlockedCount(beanInfo.getBlockedCount());
              info.setWaitedCount(beanInfo.getWaitedCount());
              
              if (allocatedBytes != null) {
                  info.setAllocatedBytes(allocatedBytes[i]);
              }

              threadDao.saveThreadInfo(vmId, agentId, info);
          }
          
      } catch (MalformedObjectNameException e) {
          e.printStackTrace();
      }
    }
    
    private class HarvesterAction implements Runnable {
        @Override
        public void run() {
            harvestData();
        }
    }
    
    synchronized void saveVmCaps() {
        
        boolean closeAfter = false;
        if (!connector.isAttached()) {
            closeAfter = true; 
            try {
                connector.attach();
            } catch (Exception ignore) {
                ignore.printStackTrace();
            }
        }

        try (MXBeanConnection connection = connector.connect()) {

            ThreadMXBean bean = getDataCollectorBean(connection);
            VMThreadMXCapabilities caps = new VMThreadMXCapabilities();

            if (bean.isThreadCpuTimeSupported())
                caps.addFeature(ThreadDao.CPU_TIME);
            if (bean.isThreadContentionMonitoringSupported())
                caps.addFeature(ThreadDao.CONTENTION_MONITOR);

            if (bean instanceof com.sun.management.ThreadMXBean) {
                com.sun.management.ThreadMXBean sunBean = (com.sun.management.ThreadMXBean) bean;
                if (sunBean.isThreadAllocatedMemorySupported())
                    caps.addFeature(ThreadDao.THREAD_ALLOCATED_MEMORY);
            }

            threadDao.saveCapabilities(vmId, agentId, caps);

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (closeAfter) {
            try {
                connector.close();
            } catch (Exception ignore) {
                ignore.printStackTrace();
            }
        }
    }
}
