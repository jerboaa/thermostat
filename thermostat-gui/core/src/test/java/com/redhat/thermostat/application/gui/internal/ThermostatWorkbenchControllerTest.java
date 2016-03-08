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

package com.redhat.thermostat.application.gui.internal;

import com.redhat.thermostat.common.ApplicationInfo;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.platform.Platform;
import com.redhat.thermostat.platform.mvc.Model;
import com.redhat.thermostat.shared.config.CommonPaths;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 */
public class ThermostatWorkbenchControllerTest {
    private final ApplicationInfo appInfo = new ApplicationInfo();

    class TestPlatform implements Platform {

        @Override
        public void queueOnApplicationThread(Runnable runnable) {
            runnable.run();
        }

        @Override
        public void queueOnViewThread(Runnable runnable) {
            runnable.run();
        }

        @Override
        public boolean isViewThread() {
            return true;
        }

        @Override
        public boolean isApplicationThread() {
            return true;
        }

        @Override
        public ApplicationService getAppService() {
            return null;
        }
    }

    private ThermostatWorkbenchView view;
    private Model model;
    private TestPlatform platform;
    private CommonPaths paths;

    private MenuHandler menuHandler;

    private ThermostatWorkbenchController createController() {
        ThermostatWorkbenchController controller = new ThermostatWorkbenchController()  {
            @Override
            MenuHandler createMenuHandler() {
                return menuHandler;
            }
        };
        controller.setCommonPaths(paths);
        controller.init(platform, model, view);
        return controller;
    }

    @Before
    public void setUp() {
        platform = new TestPlatform();
        view = Mockito.mock(ThermostatWorkbenchView.class);
        model = Mockito.mock(Model.class);
        menuHandler = Mockito.mock(MenuHandler.class);
        paths = Mockito.mock(CommonPaths.class);
    }

    @Test
    public void testSetApplicationTitleSameAsAppInfoName() {
        ThermostatWorkbenchController controller = createController();

        Mockito.verify(view).setApplicationTitle(appInfo.getName());
    }

    @Test
    public void testSetMenuHandler() {
        ThermostatWorkbenchController controller = createController();

        Mockito.verify(menuHandler).initComponents(paths);
        Mockito.verify(view).setMenuHandler(menuHandler);
        Mockito.verify(menuHandler).startService();
    }
}
