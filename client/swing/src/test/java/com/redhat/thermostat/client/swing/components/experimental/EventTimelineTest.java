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

package com.redhat.thermostat.client.swing.components.experimental;

import com.redhat.thermostat.annotations.internal.CacioTest;
import com.redhat.thermostat.common.internal.test.Bug;
import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;
import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JButtonFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import javax.swing.JFrame;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

@Category(CacioTest.class)
@RunWith(CacioFESTRunner.class)
public class EventTimelineTest {

    private EventTimeline timeline;

    private JFrame frame;
    private FrameFixture fixture;

    private PrintStream origOut;
    private PrintStream origErr;
    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;

    @BeforeClass
    public static void setupOnce() {
        FailOnThreadViolationRepaintManager.install();
    }

    @Before
    public void setup() {
        origOut = System.out;
        origErr = System.err;

        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();

        System.setOut(new PrintStream(out));
        System.setErr(new PrintStream(err));

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                timeline = new EventTimeline();

                frame = new JFrame();
                frame.add(timeline);
            }
        });
        fixture = new FrameFixture(frame);
        fixture.show();
    }

    @After
    public void teardown() {
        System.setOut(origOut);
        System.setErr(origErr);

        fixture.cleanUp();
        fixture = null;
    }

    @Test
    @Bug(id = "PR3281",
            url = "http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=3281",
            summary = "An NPE may occur when interacting with timeline controls where no JMX events are present"
    )
    public void testZoomInWithNoEvents() {
        JButtonFixture buttonFixture = fixture.button("zoomInButton");
        buttonFixture.click();

        assertNoNullPointerExceptions(out);
        assertNoNullPointerExceptions(err);
    }

    @Test
    @Bug(id = "PR3281",
            url = "http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=3281",
            summary = "An NPE may occur when interacting with timeline controls where no JMX events are present"
    )
    public void testZoomOutWithNoEvents() {
        JButtonFixture buttonFixture = fixture.button("zoomOutButton");
        buttonFixture.click();

        assertNoNullPointerExceptions(out);
        assertNoNullPointerExceptions(err);
    }

    @Test
    @Bug(id = "PR3281",
            url = "http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=3281",
            summary = "An NPE may occur when interacting with timeline controls where no JMX events are present"
    )
    public void testZoomResetWithNoEvents() {
        JButtonFixture buttonFixture = fixture.button("zoomResetButton");
        buttonFixture.click();

        assertNoNullPointerExceptions(out);
        assertNoNullPointerExceptions(err);
    }

    @Test
    @Bug(id = "PR3281",
            url = "http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=3281",
            summary = "An NPE may occur when interacting with timeline controls where no JMX events are present"
    )
    public void testMoveLeftWithNoEvents() {
        JButtonFixture buttonFixture = fixture.button("moveLeftButton");
        buttonFixture.click();

        assertNoNullPointerExceptions(out);
        assertNoNullPointerExceptions(err);
    }

    @Test
    @Bug(id = "PR3281",
            url = "http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=3281",
            summary = "An NPE may occur when interacting with timeline controls where no JMX events are present"
    )
    public void testMoveRightWithNoEvents() {
        JButtonFixture buttonFixture = fixture.button("moveRightButton");
        buttonFixture.click();

        assertNoNullPointerExceptions(out);
        assertNoNullPointerExceptions(err);
    }

    private static void assertNoNullPointerExceptions(ByteArrayOutputStream byteArrayOutputStream) {
        String contents = byteArrayOutputStream.toString();
        assertThat(contents, not(containsString("NullPointerException")));
    }

}
