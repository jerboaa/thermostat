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

package com.redhat.thermostat.client.swing.internal.vmlist.controller;

import com.redhat.thermostat.annotations.internal.CacioTest;
import com.redhat.thermostat.client.swing.internal.accordion.Accordion;
import com.redhat.thermostat.client.swing.internal.vmlist.HostTreeComponentFactory;
import com.redhat.thermostat.client.ui.ReferenceFilter;
import com.redhat.thermostat.common.internal.test.Bug;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Ref;
import com.redhat.thermostat.storage.core.VmRef;
import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;
import org.fest.swing.annotation.GUITest;
import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.FrameFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import javax.swing.JFrame;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.fail;

@Category(CacioTest.class)
@RunWith(CacioFESTRunner.class)
public class HostTreeControllerCacioTest {

    private Accordion<HostRef, VmRef> accordion;
    private DecoratorManager decoratorManager;
    private HostTreeComponentFactory componentFactory;
    private HostTreeController controller;

    private FrameFixture frameFixture;
    private JFrame window;

    @BeforeClass
    public static void beforeClass() {
        FailOnThreadViolationRepaintManager.install();
    }

    @Before
    public void setup() {
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                window = new JFrame();

                decoratorManager = new DecoratorManager();
                componentFactory = new HostTreeComponentFactory(decoratorManager, new ContextActionController());
                accordion = new Accordion<>(componentFactory);

                window.add(accordion);
                window.pack();

                controller = new HostTreeController(accordion, decoratorManager, componentFactory);
            }
        });

        frameFixture = new FrameFixture(window);
    }

    @After
    public void cleanup() {
        frameFixture.cleanUp();
    }

    @Bug(id = "PR3148",
            url = "http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=3148",
            summary = "NPE in Accordion when rapid concurrent host/vm registration and filter additions interleave")
    @Category(GUITest.class)
    @Test
    public void testThreadedAddFilters() throws InterruptedException, IOException {
        PrintStream origOut = System.out;
        PrintStream origErr = System.err;

        try (ByteArrayOutputStream outStream = new ByteArrayOutputStream();
             ByteArrayOutputStream errStream = new ByteArrayOutputStream()) {

            System.setOut(new PrintStream(outStream));
            System.setErr(new PrintStream(errStream));

            // manual testing showed a ~20% hit rate for this bug, so 50 iterations should be
            // far more than enough to catch it at least once
            for (int i = 0; i < 50; i++) {
                performThreadedAccordionTest();
            }

            String outContents = outStream.toString();
            String errContents = errStream.toString();

            assertNoExceptions(outContents);
            assertNoExceptions(errContents);

            outStream.reset();
            errStream.reset();
        } finally {
            System.setOut(origOut);
            System.setErr(origErr);
        }
    }

    private void performThreadedAccordionTest() throws InterruptedException {
        final HostRef host1 = new HostRef("host1", "host1");
        final HostRef host2 = new HostRef("host2", "host2");
        final VmRef vm1 = new VmRef(host1, "vm1", 100, "foo-vm");
        final VmRef vm2 = new VmRef(host2, "vm2", 200, "bar-vm");
        final ReferenceFilter filter = new ReferenceFilter() {
            @Override
            public boolean matches(Ref toMatch) {
                return true;
            }
        };

        Thread addHostsThread = new Thread(new Runnable() {
            @Override
            public void run() {
                controller.registerHost(host1);
                controller.registerHost(host2);
            }
        });
        Thread addVmsThread = new Thread(new Runnable() {
            @Override
            public void run() {
                controller.registerVM(vm1);
                controller.registerVM(vm2);
            }
        });
        Thread addFiltersThread = new Thread(new Runnable() {
            @Override
            public void run() {
                controller.addFilter(filter);
            }
        });

        List<Thread> threads = Arrays.asList(addHostsThread, addVmsThread, addFiltersThread);
        Collections.shuffle(threads);
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
    }

    private void assertNoExceptions(String str) {
        if (str.contains("exception") || str.contains("Exception")) {
            fail("Test failed:\n" + str);
        }
    }

}
