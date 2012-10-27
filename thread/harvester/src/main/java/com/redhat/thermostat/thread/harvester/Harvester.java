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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MalformedObjectNameException;

import com.redhat.thermostat.thread.dao.ThreadDao;

import com.redhat.thermostat.thread.model.ThreadInfoData;
import com.redhat.thermostat.thread.model.ThreadSummary;
import com.redhat.thermostat.thread.model.VMThreadCapabilities;
import com.redhat.thermostat.utils.management.MXBeanConnection;
import com.redhat.thermostat.utils.management.MXBeanConnector;

@SuppressWarnings("restriction")
class Harvester {
    
    private static final Logger logger = Logger.getLogger(Harvester.class.getSimpleName());
    
    private boolean isConnected;
    private ScheduledExecutorService threadPool;
    private ScheduledFuture<?> harvester;
    
    MXBeanConnector connector;
    
    private MXBeanConnection connection;
    private ThreadMXBean collectorBean;
    private ThreadDao threadDao;
    private String vmId;
    
    Harvester(ThreadDao threadDao, ScheduledExecutorService threadPool, String vmId) {
        this.connector = new MXBeanConnector(vmId);
        this.threadDao = threadDao;
        this.vmId = vmId;
        this.threadPool = threadPool;
    }
    
    synchronized boolean start() {
        if (isConnected) {
            return true;
        }

        if (!connector.isAttached()) {
            try {
                connector.attach();
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "can't attach", ex);
                return false;
            }
        }
      
        try {
            connection = connector.connect();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "can't connect", ex);
            return false;
        }
        
        isConnected = true;
        harvester = threadPool.scheduleAtFixedRate(new HarvesterAction(), 0, 1, TimeUnit.SECONDS);
        
        return isConnected;
    }
    
    boolean isConnected() {
        return isConnected;
    }
    
    synchronized boolean stop() {
        if (!isConnected) {
            return true;
        }
        
        harvester.cancel(false);
        isConnected = false;

        boolean stillConnected = false;
        try {
            connection.close();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "can't close connection", ex);
            stillConnected = true;
        }

        if (connector.isAttached()) {
            try {
                connector.close();
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "can't detach", ex);
                if (stillConnected) {
                    isConnected = true;
                }
                return false;
            }
        }

        return true;
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
          
          ThreadSummary summary = new ThreadSummary();
          
          collectorBean = getDataCollectorBean(connection);
          
          summary.setCurrentLiveThreads(collectorBean.getThreadCount());
          summary.setCurrentDaemonThreads(collectorBean.getDaemonThreadCount());
          summary.setTimeStamp(timestamp);
          summary.setVmId(Integer.parseInt(vmId));
          threadDao.saveSummary(summary);
          
          long [] ids = collectorBean.getAllThreadIds();
          long[] allocatedBytes = null;
          
          // now the details for the threads
          if (collectorBean instanceof com.sun.management.ThreadMXBean) {
              com.sun.management.ThreadMXBean sunBean = (com.sun.management.ThreadMXBean) collectorBean;
              boolean wasEnabled = false;
              try {
                  if (sunBean.isThreadAllocatedMemorySupported()) {
                      wasEnabled = sunBean.isThreadAllocatedMemoryEnabled();
                      sunBean.setThreadAllocatedMemoryEnabled(true);
                      allocatedBytes = sunBean.getThreadAllocatedBytes(ids);
                      sunBean.setThreadAllocatedMemoryEnabled(wasEnabled);
                  }
              } catch (Exception ignore) {}
          }

          ThreadInfo[] threadInfos = collectorBean.getThreadInfo(ids, true, true);
          
          for (int i = 0; i < ids.length; i++) {
              ThreadInfoData info = new ThreadInfoData();
              ThreadInfo beanInfo = threadInfos[i];

              info.setTimeStamp(timestamp);

              info.setThreadName(beanInfo.getThreadName());
              info.setThreadId(beanInfo.getThreadId());
              info.setState(beanInfo.getThreadState());
              info.setStackTrace(beanInfo.getStackTrace());

              info.setThreadCpuTime(collectorBean.getThreadCpuTime(info.getThreadId()));
              info.setThreadUserTime(collectorBean.getThreadUserTime(info.getThreadId()));
              
              info.setThreadBlockedCount(beanInfo.getBlockedCount());
              info.setThreadWaitCount(beanInfo.getWaitedCount());
              
              if (allocatedBytes != null) {
                  info.setAllocatedBytes(allocatedBytes[i]);
              }

              info.setVmId(Integer.parseInt(vmId));
              threadDao.saveThreadInfo(info);
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
    
    synchronized boolean saveVmCaps() {
        
        boolean closeAfter = false;
        if (!connector.isAttached()) {
            closeAfter = true; 
            try {
                connector.attach();
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "can't attach", ex);
                if (closeAfter) {
                    closeConnection();
                }
                return false;
            }
        }

        try (MXBeanConnection connection = connector.connect()) {

            ThreadMXBean bean = getDataCollectorBean(connection);
            VMThreadCapabilities caps = new VMThreadCapabilities();

            List<String> features = new ArrayList<>(3);
            if (bean.isThreadCpuTimeSupported())
                features.add(ThreadDao.CPU_TIME);
            if (bean.isThreadContentionMonitoringSupported())
                features.add(ThreadDao.CONTENTION_MONITOR);

            if (bean instanceof com.sun.management.ThreadMXBean) {
                com.sun.management.ThreadMXBean sunBean = (com.sun.management.ThreadMXBean) bean;
                
                try {
                    if (sunBean.isThreadAllocatedMemorySupported()) {
                        features.add(ThreadDao.THREAD_ALLOCATED_MEMORY);
                    }
                } catch (Exception ignore) {};
            }
            caps.setSupportedFeaturesList(features.toArray(new String[features.size()]));
            caps.setVmId(Integer.parseInt(vmId));
            threadDao.saveCapabilities(caps);

        } catch (Exception ex) {
            logger.log(Level.SEVERE, "can't get MXBeanConnection connection", ex);
            return false;
        }

        if (closeAfter) {
            closeConnection();
        }
        
        return true;
    }
    
    private void closeConnection() {
        try {
            connector.close();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "can't close connection to vm", ex);
        }
    }
}
