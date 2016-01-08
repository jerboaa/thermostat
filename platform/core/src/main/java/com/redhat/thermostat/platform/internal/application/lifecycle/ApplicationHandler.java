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

package com.redhat.thermostat.platform.internal.application.lifecycle;

import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.platform.Application;
import com.redhat.thermostat.platform.ApplicationID;
import com.redhat.thermostat.platform.ApplicationProvider;
import com.redhat.thermostat.platform.Platform;
import com.redhat.thermostat.platform.annotations.ApplicationDescriptor;
import com.redhat.thermostat.platform.internal.application.ApplicationState;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

class ApplicationHandler {

    // avoid checking annotation under testing environment
    // since providers may be mocks
    static boolean __test__ = false;

    public static enum StateChangeEvent {
        STATE_CHANGED
        ;
    }
    
    private ActionNotifier<StateChangeEvent> actionNotifier;
    
    private ApplicationProvider provider;
    
    // Testing hook
    Application application;
    
    // Testing hook
    Platform platform;
    
    void initHandler(ApplicationProvider provider) {
        this.provider = provider;
        this.actionNotifier = new ActionNotifier<>(this);

        if (__test__) {
            return;
        }

        registerApplicationDescriptor(provider);
    }

    void registerApplicationDescriptor(ApplicationProvider provider) {
        ApplicationDescriptor descriptor =
                provider.getClass().getAnnotation(ApplicationDescriptor.class);
        Class[] ids = descriptor.id();
        for (Class id : ids) {
            try {
                ApplicationID applicationID = (ApplicationID) id.newInstance();
                BundleContext ctx = FrameworkUtil.getBundle(id).getBundleContext();
                ctx.registerService(id, applicationID, null);

            } catch (Exception e) {
                // FIXME: handle that and/or exit?
                e.printStackTrace();
            }
        }
    }

    public void addStateChangeListener(ActionListener<StateChangeEvent> listener) {
        actionNotifier.addActionListener(listener);
    }
    
    public void removeStateChangeListener(ActionListener<StateChangeEvent> listener) {
        actionNotifier.removeActionListener(listener);
    }
    
    public Application create() {
        application = provider.getApplication();
        setState(ApplicationState.CREATED);
        return application;
    }
    
    public void init() {
        application.init();
        
        platform = application.getPlatform();
        setState(ApplicationState.INITIALIZED);  
    }

    public void start() {
        platform.queueOnApplicationThread(new Runnable() {
            @Override
            public void run() {
                application.start();
                setState(ApplicationState.STARTED);
            }
        });
    }
    
    public void stop() {
        platform.queueOnApplicationThread(new Runnable() {
            @Override
            public void run() {
                application.stop();
                setState(ApplicationState.STOPPED);
            }
        });
    }
    
    public void destroy() {
        platform.queueOnApplicationThread(new Runnable() {
            @Override
            public void run() {
                application.destroy();
                setState(ApplicationState.DESTROYED);
            }
        });
    }
    
    private void setState(ApplicationState state) {
        actionNotifier.fireAction(StateChangeEvent.STATE_CHANGED, state);
    }
}
