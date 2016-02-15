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

package com.redhat.thermostat.platform.internal.application.lifecycle;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.platform.ApplicationProvider;
import com.redhat.thermostat.platform.MDIService;
import com.redhat.thermostat.platform.PlatformShutdown;
import com.redhat.thermostat.platform.internal.application.ApplicationInfo;
import com.redhat.thermostat.platform.internal.application.ApplicationRegistry;
import com.redhat.thermostat.platform.internal.mvc.lifecycle.MVCLifeCycleManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import java.util.concurrent.CountDownLatch;

/**
 */
public class ApplicationLifeCycleManager implements ActionListener<ApplicationRegistry.RegistryEvent> {
    private CountDownLatch shutdownLatch;
    private Thread applicationThread;
    private ApplicationLifeCycleThread lifeCycleRunnable;
    private ApplicationInfo.Application target;

    private MVCLifeCycleManager mvcLifeCycleManager;

    public ApplicationLifeCycleManager(CountDownLatch shutdownLatch) {
        mvcLifeCycleManager = new MVCLifeCycleManager();
        init(shutdownLatch,
             new ApplicationLifeCycleThread(shutdownLatch, mvcLifeCycleManager),
             mvcLifeCycleManager);
    }

    private void init(CountDownLatch shutdownLatch,
                      ApplicationLifeCycleThread lifeCycleRunnable,
                      MVCLifeCycleManager mvcLifeCycleManager)
    {
        this.shutdownLatch = shutdownLatch;
        this.lifeCycleRunnable = lifeCycleRunnable;
        this.mvcLifeCycleManager = mvcLifeCycleManager;
        this.applicationThread = createThread(lifeCycleRunnable);
    }

    // Testing hook
    ApplicationLifeCycleManager(CountDownLatch shutdownLatch,
                                ApplicationLifeCycleThread lifeCycleRunnable,
                                MVCLifeCycleManager mvcLifeCycleManager)
    {
        init(shutdownLatch, lifeCycleRunnable, mvcLifeCycleManager);
    }
    
    // Testing hook
    Thread createThread(ApplicationLifeCycleThread lifeCycleRunnable) {
        Thread thread = new Thread(lifeCycleRunnable);
        thread.setName("Application Lifecycle Thread");
        return thread;
    }

    public void registerShutdownService() {
        BundleContext context =
                FrameworkUtil.getBundle(PlatformShutdown.class).getBundleContext();
        context.registerService(PlatformShutdown.class, lifeCycleRunnable, null);
    }

    public void registerMDIService() {
        BundleContext context =
                FrameworkUtil.getBundle(MDIService.class).getBundleContext();
        context.registerService(MDIService.class, mvcLifeCycleManager, null);
    }

    public void execute() {
        applicationThread.start();
        try {
            shutdownLatch.await();
        } catch (InterruptedException ignored) {}
    }

    @Override
    public void actionPerformed(ActionEvent<ApplicationRegistry.RegistryEvent> actionEvent) {
        ApplicationRegistry registry = (ApplicationRegistry) actionEvent.getSource();
        checkTarget(registry);
    }

    public void setTarget(ApplicationInfo.Application target) {
        this.target = target;
    }

    private void checkTarget(ApplicationRegistry registry) {
        if (target == null) {
            return;
        }

        if (registry.containsProvider(target.provider)) {
            create(registry.getProvider(target.provider));
        }
    }

    void create(ApplicationProvider provider) {
        lifeCycleRunnable.create(provider);
    }
    
    public void shutdown() {
        lifeCycleRunnable.commenceShutdown();
    }
}
