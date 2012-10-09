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

package com.redhat.thermostat.client.heap;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.client.common.views.ViewFactory;
import com.redhat.thermostat.client.osgi.service.ApplicationService;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.appctx.ApplicationContextUtil;
import com.redhat.thermostat.common.heap.HeapDump;
import com.redhat.thermostat.common.heap.ObjectHistogram;

public class HeapDumpDetailsControllerTest {

    private HeapDumpDetailsView view;

    @Before
    public void setUp() {
        ApplicationContextUtil.resetApplicationContext();

        // view factory
        ViewFactory viewFactory = mock(ViewFactory.class);

        view = mock(HeapDumpDetailsView.class);
        when(viewFactory.getView(HeapDumpDetailsView.class)).thenReturn(view);

        HeapHistogramView histogramView = mock(HeapHistogramView.class);
        when(viewFactory.getView(HeapHistogramView.class)).thenReturn(histogramView);

        ObjectDetailsView objectView = mock(ObjectDetailsView.class);
        when(viewFactory.getView(ObjectDetailsView.class)).thenReturn(objectView);

        ApplicationContext.getInstance().setViewFactory(viewFactory);
    }

    @After
    public void tearDown() {
        ApplicationContextUtil.resetApplicationContext();
    }

    @Test
    public void verifyInitialize() throws IOException {
        ApplicationService appService = mock(ApplicationService.class);

        ObjectHistogram histogram = mock(ObjectHistogram.class);

        HeapDump dump = mock(HeapDump.class);
        when(dump.getHistogram()).thenReturn(histogram);


        HeapDumpDetailsController controller = new HeapDumpDetailsController(appService);
        controller.setDump(dump);

        verify(dump).searchObjects(isA(String.class), anyInt());
        verify(view).addSubView(isA(String.class), isA(HeapHistogramView.class));
        verify(view).addSubView(isA(String.class), isA(ObjectDetailsView.class));
    }

}
