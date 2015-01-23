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

package com.redhat.thermostat.thread.client.common.osgi;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.thread.client.common.collector.ThreadCollectorFactory;
import com.redhat.thermostat.thread.client.common.collector.impl.ThreadCollectorFactoryImpl;
import com.redhat.thermostat.thread.dao.ThreadDao;

public class Activator implements BundleActivator {
    
    private ThreadCollectorFactoryImpl collectorFactory;
    private ServiceTracker agentInfoDaoTracker;
    private ServiceTracker threadDaoTracker;
    
    @Override
    public void start(final BundleContext context) throws Exception {
        
        collectorFactory = new ThreadCollectorFactoryImpl();
        context.registerService(ThreadCollectorFactory.class.getName(), collectorFactory, null);

        agentInfoDaoTracker = new ServiceTracker(context, AgentInfoDAO.class.getName(), null) {
            @Override
            public Object addingService(ServiceReference reference) {
                AgentInfoDAO agentDao = (AgentInfoDAO) context.getService(reference);
                collectorFactory.setAgentDao(agentDao);
                return super.addingService(reference);
            }

            @Override
            public void removedService(ServiceReference reference, Object service) {
                collectorFactory.setAgentDao(null);
                context.ungetService(reference);
                super.removedService(reference, service);
            }
        };
        agentInfoDaoTracker.open();

        threadDaoTracker = new ServiceTracker(context, ThreadDao.class.getName(), null) {
            @Override
            public Object addingService(ServiceReference reference) {
                ThreadDao threadDao = (ThreadDao) context.getService(reference);
                collectorFactory.setThreadDao(threadDao);
                return super.addingService(reference);
            }

            @Override
            public void removedService(ServiceReference reference, Object service) {
                collectorFactory.setThreadDao(null);
                context.ungetService(reference);
                super.removedService(reference, service);
            }
        };
        threadDaoTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        agentInfoDaoTracker.close();
        threadDaoTracker.close();
    }
}

