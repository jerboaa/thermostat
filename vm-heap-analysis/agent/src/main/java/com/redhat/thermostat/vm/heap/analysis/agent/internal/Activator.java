/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.vm.heap.analysis.agent.internal;

import java.util.Map;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.redhat.thermostat.agent.command.ReceiverRegistry;
import com.redhat.thermostat.agent.utils.management.MXBeanConnectionPool;
import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.MultipleServiceTracker.Action;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDAO;

public class Activator implements BundleActivator {

    private ReceiverRegistry receivers;

    private HeapDumpReceiver receiver = null;

    private MultipleServiceTracker tracker;

    @Override
    public void start(BundleContext context) {
        receivers = new ReceiverRegistry(context);
        
        Class<?>[] deps = new Class<?>[] {
                HeapDAO.class,
                MXBeanConnectionPool.class,
                WriterID.class // heap dump receiver uses it.
        };

        tracker = new MultipleServiceTracker(context, deps, new Action() {

            @Override
            public void dependenciesAvailable(Map<String, Object> services) {
                HeapDAO service = (HeapDAO) services.get(HeapDAO.class.getName());
                MXBeanConnectionPool pool = (MXBeanConnectionPool) services.get(MXBeanConnectionPool.class.getName());
                WriterID writerId = (WriterID) services.get(WriterID.class.getName());
                receiver = new HeapDumpReceiver(service, pool, writerId);
                receivers.registerReceiver(receiver);
            }

            @Override
            public void dependenciesUnavailable() {
                receivers.unregisterReceivers();
            }
            
        });
        tracker.open();
    }

    @Override
    public void stop(BundleContext context) {
        receivers.unregisterReceivers();
        tracker.close();
    }

}

