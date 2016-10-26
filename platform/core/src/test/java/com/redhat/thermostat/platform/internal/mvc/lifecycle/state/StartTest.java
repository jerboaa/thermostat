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

package com.redhat.thermostat.platform.internal.mvc.lifecycle.state;

import com.redhat.thermostat.platform.Platform;
import com.redhat.thermostat.platform.mvc.Controller;
import com.redhat.thermostat.platform.mvc.MVCProvider;
import com.redhat.thermostat.platform.mvc.Model;
import com.redhat.thermostat.platform.mvc.View;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 */
public class StartTest {

    private MVCProvider provider;
    private Platform platform;
    private StateMachineTransitionDispatcher dispatcher;
    private Context context;
    private View view;
    private Controller controller;
    private Model model;

    @Before
    public void setUp() {
        provider = mock(MVCProvider.class);
        platform = mock(Platform.class);
        dispatcher = mock(StateMachineTransitionDispatcher.class);

        view = mock(View.class);
        model = mock(Model.class);
        controller = mock(Controller.class);

        when(provider.getView()).thenReturn(view);
        when(provider.getController()).thenReturn(controller);
        when(provider.getModel()).thenReturn(model);

        context = new Context();
        context.dispatcher = dispatcher;
        context.provider = provider;
        context.platform = platform;
    }

    @Test
    public void execute() throws Exception {

        ArgumentCaptor<Runnable> captor0 = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Runnable> captor1 = ArgumentCaptor.forClass(Runnable.class);

        doNothing().when(platform).queueOnApplicationThread(captor0.capture());
        doNothing().when(platform).queueOnViewThread(captor1.capture());

        Start start = new Start();
        start.execute(context);

        captor0.getValue().run();
        verify(controller).start();

        captor1.getValue().run();
        verify(view).start();

        captor0.getValue().run();
        verify(controller).viewStarted();
    }
}