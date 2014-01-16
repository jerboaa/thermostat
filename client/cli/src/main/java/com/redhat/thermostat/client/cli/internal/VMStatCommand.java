/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.client.cli.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.redhat.thermostat.client.cli.HostVMArguments;
import com.redhat.thermostat.client.cli.VMStatPrintDelegate;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.VmRef;

public class VMStatCommand extends AbstractCommand {

    private static final Logger log = LoggingUtils.getLogger(VMStatCommand.class);
    
    private List<VMStatPrintDelegate> delegates;
    private BundleContext context;
    
    public VMStatCommand() {
        this(FrameworkUtil.getBundle(VMStatCommand.class).getBundleContext());
    }

    VMStatCommand(BundleContext context) {
        this.context = context;
        delegates = new CopyOnWriteArrayList<>();
        ServiceTracker tracker = new ServiceTracker(context, VMStatPrintDelegate.class.getName(), null) {

            public Object addingService(ServiceReference reference) {
                VMStatPrintDelegate delegate = (VMStatPrintDelegate) super.addingService(reference);
                delegates.add(delegate);
                return delegate;
            };

            public void removedService(ServiceReference reference, Object service) {
                delegates.remove(service);
                super.removedService(reference, service);
            };

        };
        tracker.open();
    }

    @Override
    public void run(final CommandContext ctx) throws CommandException {
        HostVMArguments hostVMArgs = new HostVMArguments(ctx.getArguments());
        VmRef vm = hostVMArgs.getVM();
        // Pass a copy of the delegates list to the printer
        final VMStatPrinter statPrinter = new VMStatPrinter(vm, new ArrayList<>(delegates), ctx.getConsole().getOutput());
        statPrinter.printStats();
        boolean continuous = ctx.getArguments().hasArgument("continuous");
        if (continuous) {
            startContinuousStats(ctx, statPrinter);
        }
    }

    private void startContinuousStats(final CommandContext ctx, final VMStatPrinter statPrinter) {

        final CountDownLatch latch = new CountDownLatch(1);
        ServiceReference ref = context.getServiceReference(ApplicationService.class.getName());
        ApplicationService appSvc = (ApplicationService) context.getService(ref);
        Timer timer = appSvc.getTimerFactory().createTimer();
        timer.setDelay(1);
        timer.setInitialDelay(1);
        timer.setSchedulingType(Timer.SchedulingType.FIXED_RATE);
        timer.setTimeUnit(TimeUnit.SECONDS);
        timer.setAction(new Runnable() {

            @Override
            public void run() {
                statPrinter.printUpdatedStats();
            }
        });
        timer.start();
        Thread t = new Thread() {
            public void run() {
                try {
                    ctx.getConsole().getInput().read();
                } catch (IOException e) {
                    log.log(Level.WARNING, "Unexpected IOException while waiting for user input", e);
                } finally {
                    latch.countDown();
                }
            }
        };
        t.start();
        try {
            latch.await();
            timer.stop();
        } catch (InterruptedException e) {
            // Return immediately.
        }
        
        context.ungetService(ref);
    }

}

