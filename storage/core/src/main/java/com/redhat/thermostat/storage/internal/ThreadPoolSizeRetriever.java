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

package com.redhat.thermostat.storage.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;

public class ThreadPoolSizeRetriever {
    
    private static final Logger logger = LoggingUtils.getLogger(ThreadPoolSizeRetriever.class);
    
    /*
     * Integer. The size of the desired thread pool size.
     */
    static final String THREAD_POOL_SIZE = "com.redhat.thermostat.storage.queue.poolSize";
    
    /*
     * Boolean. If set to true no bound is enforced. Otherwise an upper bound
     * of DEFAULT_THREAD_POOL_SIZE_MAX is enforced.
     */
    static final String THREAD_POOL_SIZE_UNBOUNDED = "com.redhat.thermostat.storage.queue.unbounded";
    
    /*
     * The configured upper bound without the THREAD_POOL_SIZE_UNBOUNDED
     * property set.
     *
     *
     * Default process limit (ulimit -u) on Linux is 1024. Since this includes
     * threads (threads are tasks on Linux), consider the following reasoning.
     * 
     * We assume that for a current Linux desktop system one needs approximately
     * 400 processes to run. Thus, leaves 624 processes for a user system with
     * a limit of 1024. Next, MongoClient uses a default thread pool with a max
     * thread limit of 100. Jetty 9 (web-storage-service) uses a default thread
     * pool limit of 200.
     * 
     * So if we were to use a thread limit of 250 for queued storage this would
     * mean for the web-storage-service case (ignoring other Java threads):
     * 250 threads for web-client (agent) + 250 threads for backing storage 
     * (the webapp) + 200 threads Jetty uses as dispatch threads + 100 threads
     * (mongodb creates one thread per connection) equals to 800 threads.
     * 800 + 400 = 1200 which is beyond the default user process
     * ulimit of 1024. Note that thermostat clients would likely create only
     * a few threads, since they mostly do reads (and queries are not queued).
     * 
     * Hence a default of 100 should work well: 100 (agent) + 100 (webapp) +
     * 100 (mongodb) + 200 (jetty) = 500. 500 + 400 = 900. 900 < 1024
     */
    static final int DEFAULT_THREAD_POOL_SIZE_MAX = 100;
    
    /*
     * The default thread pool size if no properties are set to override it.
     */
    static final int DEFAULT_THREAD_POOL_SIZE = determineDefaultThreadPoolSize();

    public int getPoolSize() {
        Integer candidate = getPoolSizeFromProperty();
        if (candidate == null) {
            logger.log(Level.CONFIG, THREAD_POOL_SIZE + " system property unset."
                    + " Using default: " + DEFAULT_THREAD_POOL_SIZE);
            return DEFAULT_THREAD_POOL_SIZE;
        }
        if (candidate <= 0) {
            throw new InvalidConfigurationException("Value of property " +
                    THREAD_POOL_SIZE +": " + candidate + " <= 0");
        }
        if (isPoolSizeCapped() && candidate > DEFAULT_THREAD_POOL_SIZE_MAX) {
            throw new InvalidConfigurationException("Value of property " +
                    THREAD_POOL_SIZE +": " + candidate + " > " + DEFAULT_THREAD_POOL_SIZE_MAX
                    + " and property " + THREAD_POOL_SIZE_UNBOUNDED + " unset or set to false");
        }
        logger.log(Level.CONFIG, "Using a thread pool size of " + candidate + " for QueuedStorage");
        return candidate;
    }

    private static int determineDefaultThreadPoolSize() {
        // Make the number of default thread pool size a function of available
        // processors.
        return Runtime.getRuntime().availableProcessors() * 2;
    }

    private Integer getPoolSizeFromProperty() {
        return Integer.getInteger(THREAD_POOL_SIZE);
    }
    
    private boolean isPoolSizeCapped() {
        return !Boolean.getBoolean(THREAD_POOL_SIZE_UNBOUNDED);
    }
}
