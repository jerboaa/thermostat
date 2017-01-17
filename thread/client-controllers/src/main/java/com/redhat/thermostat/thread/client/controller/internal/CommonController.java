/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.thread.client.controller.internal;

import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.client.core.views.BasicView.Action;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.thread.client.controller.internal.cache.AppCache;
import com.redhat.thermostat.thread.client.controller.internal.cache.AppCacheKey;
import com.redhat.thermostat.thread.model.SessionID;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public abstract class CommonController {

    public static final long PERIOD = 1_000;

    private static final String __LOCK__ = new String("CommonController.lock");

    public static final AppCacheKey THREAD_STATE_CACHE = new AppCacheKey("THREAD_STATE_CACHE", CommonController.class);

    protected Timer timer;
    protected BasicView view;
    protected AppCache cache;

    protected volatile SessionID session;

    public CommonController(Timer timer, BasicView view, AppCache cache) {
        this.view = view;
        this.timer = timer;
        this.cache = cache;
    }

    protected final <V> V executeInCriticalSection(Callable<V> callable) {
        synchronized (__LOCK__) {
            try {
                return callable.call();

            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    void initialize() {
        timer.setInitialDelay(0);
        timer.setDelay(PERIOD);
        timer.setTimeUnit(TimeUnit.MILLISECONDS);
        timer.setSchedulingType(SchedulingType.FIXED_RATE);
        
        view.addActionListener(new ActionListener<Action>() {
            @Override
            public void actionPerformed(ActionEvent<Action> actionEvent) {
                switch (actionEvent.getActionId()) {
                case VISIBLE:
                    onViewVisible();
                    timer.start();
                    break;

                case HIDDEN:
                    timer.stop();
                    onViewHidden();
                    break;

                default:
                    break;
                }
            }
        });
    }

    protected void onViewVisible() {}
    protected void onViewHidden() {}

    public void setSession(final SessionID session) {
        executeInCriticalSection(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                CommonController.this.session = session;
                return null;
            }
        });
    }

    long __test__getTimeDeltaOnNewSession() {
        return -1;
    }
}

