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

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.redhat.thermostat.thread.client.controller.internal.cache.AppCache;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.thread.client.common.view.LockView;
import com.redhat.thermostat.thread.dao.LockInfoDao;
import com.redhat.thermostat.thread.model.LockInfo;

public class LockControllerTest {

    private LockView view;
    private Timer timer;
    private LockInfoDao dao;
    private VmRef vm;
    private AppCache cache;

    @Before
    public void setup() {
        view = mock(LockView.class);
        timer = mock(Timer.class);
        dao = mock(LockInfoDao.class);
        vm = mock(VmRef.class);
        cache = mock(AppCache.class);
    }

    @Test
    public void verifyVisibilityEnablesAndDisablesTimer() {
        LockController controller = new LockController(view, timer, dao, vm, cache);
        controller.initialize();

        ArgumentCaptor<ActionListener> actionListenerCaptor = ArgumentCaptor.forClass(ActionListener.class);
        verify(view).addActionListener(actionListenerCaptor.capture());

        ActionListener listener = actionListenerCaptor.getValue();
        listener.actionPerformed(new ActionEvent(view, BasicView.Action.VISIBLE));

        verify(timer).start();

        listener.actionPerformed(new ActionEvent(view, BasicView.Action.HIDDEN));

        verify(timer).stop();
    }

    @Test
    public void verifyViewIsNotUpdatedOnNoData() {
        when(dao.getLatestLockInfo(vm)).thenReturn(null);

        LockController controller = new LockController(view, timer, dao, vm, cache);
        controller.initialize();

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(timer).setAction(captor.capture());
        Runnable updater = captor.getValue();

        verify(view, never()).setLatestLockData(isA(LockInfo.class));

        updater.run();

        verify(view, never()).setLatestLockData(isA(LockInfo.class));
    }

    @Test
    public void verifyViewIsUpdatedWithData() {
        LockInfo lockInfo = new LockInfo();
        when(dao.getLatestLockInfo(vm)).thenReturn(lockInfo);

        LockController controller = new LockController(view, timer, dao, vm, cache);
        controller.initialize();

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(timer).setAction(captor.capture());
        Runnable updater = captor.getValue();

        verify(view, never()).setLatestLockData(isA(LockInfo.class));

        updater.run();

        verify(view).setLatestLockData(lockInfo);
    }
}
