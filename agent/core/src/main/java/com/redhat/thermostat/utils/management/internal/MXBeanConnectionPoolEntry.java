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

package com.redhat.thermostat.utils.management.internal;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

// Container class used for pool map entries
class MXBeanConnectionPoolEntry {
    
    // Timeout when waiting for JMX url from agent proxy
    private static final long TIMEOUT_MS = 10000L;
    
    private final int pid;
    private int usageCount;
    private String jmxUrl;
    private MXBeanConnectionImpl connection;
    private CountDownLatch urlLatch;
    private Exception ex;
    
    MXBeanConnectionPoolEntry(int pid) {
        this(pid, new CountDownLatch(1));
    }
    
    MXBeanConnectionPoolEntry(int pid, CountDownLatch urlLatch) {
        this.pid = pid;
        this.usageCount = 1;
        this.jmxUrl = null;
        this.connection = null;
        this.urlLatch = urlLatch;
    }
    
    MXBeanConnectionImpl getConnection() {
        return connection;
    }
    
    String getJmxUrlOrBlock() throws IOException, InterruptedException {
        boolean finished = urlLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (!finished) {
            throw new IOException("Timed out while waiting for JMX service URL");
        }
        if (ex != null) {
            throw new IOException("Failed to get JMX service URL", ex);
        }
        return jmxUrl;
    }
    
    int getPid() {
        return pid;
    }
    
    int getUsageCount() {
        return usageCount;
    }
    
    void incrementUsageCount() {
        this.usageCount++;
    }
    
    void decrementUsageCount() {
        this.usageCount--;
    }
    
    void setJmxUrl(String jmxUrl) {
        this.jmxUrl = jmxUrl;
        urlLatch.countDown();
    }
    
    void setConnection(MXBeanConnectionImpl connection) {
        this.connection = connection;
    }
    
    void setException(Exception ex) {
        this.ex = ex;
        // No JMX URL coming, stop waiting and throw exception
        urlLatch.countDown();
    }
    
}
