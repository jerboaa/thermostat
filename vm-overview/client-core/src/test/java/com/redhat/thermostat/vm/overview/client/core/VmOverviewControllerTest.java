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

package com.redhat.thermostat.vm.overview.client.core;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.client.core.views.BasicView.Action;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.appctx.ApplicationContextUtil;
import com.redhat.thermostat.common.dao.VmInfoDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.vm.overview.client.locale.LocaleResources;

public class VmOverviewControllerTest {
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    
    private static final int VM_PID = 1337;
    private static final long START_TIME = 10000;
    private static final long STOP_TIME = 20000;
    private static final String JAVA_VERSION = "1.2000.1";
    private static final String JAVA_HOME = "/path/to/java";
    private static final String MAIN_CLASS = "Main";
    private static final String COMMAND_LINE = "java Main command line args";
    private static final String VM_NAME = "MyCoolJVM";
    private static final String VM_INFO = "Info about MyCoolJVM";
    private static final String VM_VERSION = "1.0";
    private static final String VM_ARGS = "-Dvar=arg -DotherVar=otherArg";
    private static final Map<String, String> PROPS = Collections.emptyMap();
    private static final Map<String, String> ENV = Collections.emptyMap();
    private static final String[] LIBS = new String[0];

    private Timer timer;
    private Runnable timerAction;
    private VmOverviewView view;
    private ActionListener<VmOverviewView.Action> listener;
    private VmOverviewController controller;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Before
    public void setUp() {
        ApplicationContextUtil.resetApplicationContext();

        // Setup timer
        timer = mock(Timer.class);
        ArgumentCaptor<Runnable> timerActionCaptor = ArgumentCaptor
                .forClass(Runnable.class);
        doNothing().when(timer).setAction(timerActionCaptor.capture());

        TimerFactory timerFactory = mock(TimerFactory.class);
        when(timerFactory.createTimer()).thenReturn(timer);
        ApplicationContext.getInstance().setTimerFactory(timerFactory);

        // Setup DAOs
        VmInfo vmInfo = new VmInfo(VM_PID, START_TIME, STOP_TIME, JAVA_VERSION,
                JAVA_HOME, MAIN_CLASS, COMMAND_LINE, VM_NAME, VM_INFO,
                VM_VERSION, VM_ARGS, PROPS, ENV, LIBS);

        VmRef ref = mock(VmRef.class);

        VmInfoDAO vmInfoDao = mock(VmInfoDAO.class);
        when(vmInfoDao.getVmInfo(any(VmRef.class))).thenReturn(vmInfo);

        // Setup View
        ArgumentCaptor<ActionListener> listenerCaptor = ArgumentCaptor
                .forClass(ActionListener.class);
        view = mock(VmOverviewView.class);
        doNothing().when(view).addActionListener(listenerCaptor.capture());
        VmOverviewViewProvider viewProvider = mock(VmOverviewViewProvider.class);
        when(viewProvider.createView()).thenReturn(view);

        controller = new VmOverviewController(vmInfoDao, ref, viewProvider);

        listener = listenerCaptor.getValue();
        timerAction = timerActionCaptor.getValue();
    }

    @After
    public void tearDown() {
        ApplicationContextUtil.resetApplicationContext();
    }

    @Test
    public void verifyViewIsUpdatedWithData() {
        timerAction.run();

        DateFormat timestampFormat = controller.getDateFormat();
        verify(view).setVmPid(eq(String.valueOf(VM_PID)));
        verify(view).setVmStartTimeStamp(eq(timestampFormat.format(new Date(START_TIME))));
        verify(view).setVmStopTimeStamp(eq(timestampFormat.format(new Date(STOP_TIME))));
        verify(view).setJavaVersion(eq(JAVA_VERSION));
        verify(view).setJavaHome(eq(JAVA_HOME));
        verify(view).setMainClass(eq(MAIN_CLASS));
        verify(view).setJavaCommandLine(eq(COMMAND_LINE));
        
        verify(view).setVmNameAndVersion(eq(translator.localize(LocaleResources.VM_INFO_VM_NAME_AND_VERSION,
                        VM_NAME, VM_VERSION, VM_INFO)));
        verify(view).setVmArguments(eq(VM_ARGS));
    }

    @Test
    public void verifyTimerIsSetUpCorrectly() {
        assertNotNull(timer);

        verify(timer).setAction(isNotNull(Runnable.class));
        verify(timer).setDelay(5);
        verify(timer).setTimeUnit(TimeUnit.SECONDS);
        verify(timer).setInitialDelay(0);
        verify(timer).setSchedulingType(SchedulingType.FIXED_RATE);
    }

    @Test
    public void verifyTimerRunsWhenNeeded() {
        listener.actionPerformed(new ActionEvent<>(view, Action.VISIBLE));

        verify(timer).start();

        listener.actionPerformed(new ActionEvent<>(view, Action.HIDDEN));

        verify(timer).stop();
    }

}
