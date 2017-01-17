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

package com.redhat.thermostat.platform.internal.mvc.lifecycle.handlers;

import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.platform.Platform;
import com.redhat.thermostat.platform.annotations.Extension;
import com.redhat.thermostat.platform.annotations.ExtensionPoint;
import com.redhat.thermostat.platform.mvc.Controller;
import com.redhat.thermostat.platform.mvc.MVCProvider;
import com.redhat.thermostat.platform.mvc.Model;
import com.redhat.thermostat.platform.mvc.View;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 */
public class MVCExtensionLinkerTest {

    private interface TestExtension {
        String saySomething();
    }

    private class TestMVC implements MVCProvider {
        String saidSomething = "";
        Controller controller = new Controller() {

            @ExtensionPoint(TestExtension.class)
            private void addTest(TestExtension extension) {
                saidSomething = extension.saySomething();
            }
        };
        View view = new View();
        Model model = new Model();

        @Override
        public Controller getController() {
            return controller;
        }

        @Override
        public View getView() {
            return view;
        }

        @Override
        public Model getModel() {
            return model;
        }
    }

    @Extension(TestExtension.class)
    private class ExtendedController extends Controller implements TestExtension {
        @Override
        public String saySomething() {
            return "Huzza!";
        }
    }

    private class ExtensionForTest implements MVCProvider {
        Controller controller = new ExtendedController();
        View view = new View();
        Model model = new Model();

        @Override
        public Controller getController() {
            return controller;
        }

        @Override
        public View getView() {
            return view;
        }

        @Override
        public Model getModel() {
            return model;
        }
    }

    @Test
    public void link() throws Exception {
        TestPlatform platform = new TestPlatform();
        MVCExtensionLinker linker = new MVCExtensionLinker();
        linker.setPlatform(platform);

        TestMVC testMVC = new TestMVC();
        linker.link(testMVC);

        assertEquals("", testMVC.saidSomething);
        assertNull(platform.applicationRunnable);
        assertNull(platform.viewRunnable);

        ExtensionForTest extensionForTest = new ExtensionForTest();
        linker.link(extensionForTest);

        assertNotNull(platform.applicationRunnable);
        assertNull(platform.viewRunnable);
        platform.applicationRunnable.run();
        platform.applicationRunnable = null;

        assertEquals("Huzza!", testMVC.saidSomething);
    }

    @Test
    public void linkProviderRegisteredAfterUser() throws Exception {
        TestPlatform platform = new TestPlatform();
        MVCExtensionLinker linker = new MVCExtensionLinker();
        linker.setPlatform(platform);

        ExtensionForTest extensionForTest = new ExtensionForTest();
        linker.link(extensionForTest);

        assertNull(platform.applicationRunnable);
        assertNull(platform.viewRunnable);

        TestMVC testMVC = new TestMVC();
        linker.link(testMVC);

        assertNotNull(platform.applicationRunnable);
        assertNull(platform.viewRunnable);
        platform.applicationRunnable.run();
        platform.applicationRunnable = null;

        assertEquals("Huzza!", testMVC.saidSomething);
    }

    private class TestPlatform implements Platform {

        Runnable applicationRunnable;
        Runnable viewRunnable;

        @Override
        public void queueOnApplicationThread(Runnable runnable) {
            applicationRunnable = runnable;
        }

        @Override
        public void queueOnViewThread(Runnable runnable) {
            viewRunnable = runnable;
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
}