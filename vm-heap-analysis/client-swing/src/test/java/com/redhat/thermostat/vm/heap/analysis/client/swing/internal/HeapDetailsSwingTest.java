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

package com.redhat.thermostat.vm.heap.analysis.client.swing.internal;

import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Callable;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.JLabel;

import org.fest.swing.annotation.GUITest;
import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JTabbedPaneFixture;
import org.fest.swing.util.Triple;
import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.redhat.thermostat.annotations.internal.CacioTest;
import com.redhat.thermostat.client.swing.EdtHelper;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapHistogramView;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapTreeMapView;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectDetailsView;
import com.redhat.thermostat.vm.heap.analysis.client.locale.LocaleResources;

@Category(CacioTest.class)
@RunWith(CacioFESTRunner.class)
public class HeapDetailsSwingTest {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private JFrame frame;
    private FrameFixture frameFixture;
    private JTabbedPaneFixture tabPane;
    private HeapDetailsSwing view;

    @BeforeClass
    public static void setUpOnce() {
        FailOnThreadViolationRepaintManager.install();
    }

    @Before
    public void setUp() {
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                view = new HeapDetailsSwing();
                frame = new JFrame();
                frame.add(view.getUiComponent());

            }
        });
        frameFixture = new FrameFixture(frame);
    }

    @After
    public void tearDown() {
        frameFixture.cleanUp();
        frameFixture = null;
    }

    @GUITest
    @Test
    public void testSetDump() throws InvocationTargetException, InterruptedException {
        frameFixture.show();
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                final JLabel label = view.getLabel();
                assertNotNull(label);
                view.setDumpName(translator.localize(LocaleResources.HEAP_DUMP_LABEL, "0", "[Foo Bar]"));
                Assert.assertEquals(label.getText(), "0 - [Foo Bar]");
                return;
            }
        });

    }

    @GUITest
    @Test
    public void testTabSetup() throws InvocationTargetException, InterruptedException {
        frameFixture.show();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                tabPane = frameFixture.tabbedPane("tabs");
                assertNotNull(tabPane);

                ArrayList<String> tabTitles = new ArrayList<>();
                tabTitles.addAll(Arrays.asList(tabPane.tabTitles()));

                assertEquals(0, tabPane.component().getSelectedIndex());
                assertEquals(3, tabTitles.size());
                assertTrue(tabTitles.contains(
                        translator.localize(LocaleResources.HEAP_DUMP_SECTION_TREEMAP).getContents()));
                assertTrue(tabTitles.contains(
                        translator.localize(LocaleResources.HEAP_DUMP_SECTION_OBJECT_BROWSER).getContents()));
                assertTrue(tabTitles.contains(
                        translator.localize(LocaleResources.HEAP_DUMP_SECTION_HISTOGRAM).getContents()));
            }
        });
    }

    @GUITest
    @Test
    public void testUpdateView() throws InvocationTargetException, InterruptedException {
        frameFixture.show();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                tabPane = frameFixture.tabbedPane("tabs");
                assertNotNull(tabPane);
                assertEquals(0, tabPane.component().getSelectedIndex());
                assertEquals(3, tabPane.component().getTabCount());

                assertNull(tabPane.component().getTabComponentAt(0));
                assertNull(tabPane.component().getTabComponentAt(1));
                assertNull(tabPane.component().getTabComponentAt(2));
            }
        });

        Triple<HistogramPanel, ObjectDetailsPanel, SwingHeapTreeMapView> setup = setupViews();

        String exceptionMessage = "";
        try {
            view.updateView(null, setup.ii, setup.iii);
        } catch (IllegalArgumentException e) {
            exceptionMessage = e.getMessage();
        }
        assertEquals("component is not swing", exceptionMessage);

        exceptionMessage = "";
        try {
            view.updateView(setup.i, null, setup.iii);
        } catch (IllegalArgumentException e) {
            exceptionMessage = e.getMessage();
        }
        assertEquals("component is not swing", exceptionMessage);

        exceptionMessage = "";
        try {
            view.updateView(setup.i, setup.ii, null);
        } catch (IllegalArgumentException e) {
            exceptionMessage = e.getMessage();
        }
        assertEquals("component is not swing", exceptionMessage);

        try {
            view.updateView(setup.i, setup.ii, setup.iii);
        } catch (IllegalArgumentException e) {
            Assert.fail("no illegal argument exception expected");
        }

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                assertEquals(3, tabPane.component().getTabCount());
            }
        });
    }

    @GUITest
    @Test
    public void testAddRemove() throws InvocationTargetException, InterruptedException {
        frameFixture.show();

        final ArrayList<String> tabNames = new ArrayList<>();
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                tabPane = frameFixture.tabbedPane("tabs");
                assertNotNull(tabPane);
                tabNames.addAll(Arrays.asList(tabPane.tabTitles()));
            }
        });

        for (String tabName : tabNames) {
            view.removeSubView(new LocalizedString(tabName));
        }

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                assertEquals(0, tabPane.tabTitles().length);
            }
        });

        String exceptionMessage = "";
        try {
            view.addSubView(new LocalizedString("test1"), (HeapHistogramView) null);
        } catch (IllegalArgumentException e) {
            exceptionMessage = e.getMessage();
        }
        assertEquals("component is not swing", exceptionMessage);

        exceptionMessage = "";
        try {
            view.addSubView(new LocalizedString("test2"), (ObjectDetailsView) null);
        } catch (IllegalArgumentException e) {
            exceptionMessage = e.getMessage();
        }
        assertEquals("component is not swing", exceptionMessage);

        exceptionMessage = "";
        try {
            view.addSubView(new LocalizedString("test3"), (HeapTreeMapView) null);
        } catch (IllegalArgumentException e) {
            exceptionMessage = e.getMessage();
        }
        assertEquals("component is not swing", exceptionMessage);

        Triple<HistogramPanel, ObjectDetailsPanel, SwingHeapTreeMapView> setup = setupViews();
        try {
            view.addSubView(new LocalizedString("test1"), setup.i);
            view.addSubView(new LocalizedString("test2"), setup.ii);
            view.addSubView(new LocalizedString("test3"), setup.iii);
        } catch (IllegalArgumentException e) {
            Assert.fail("no illegal argument exception expected");
        }

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                ArrayList<String> tabTitles = new ArrayList<>();
                tabTitles.addAll(Arrays.asList(tabPane.tabTitles()));

                assertEquals(3, tabTitles.size());
                assertTrue(tabTitles.contains("test1"));
                assertTrue(tabTitles.contains("test2"));
                assertTrue(tabTitles.contains("test3"));
            }
        });
    }


    private Triple<HistogramPanel, ObjectDetailsPanel, SwingHeapTreeMapView> setupViews() {
        HistogramPanel histogramView = mock(HistogramPanel.class);
        ObjectDetailsPanel objectDetailsView = mock(ObjectDetailsPanel.class);
        SwingHeapTreeMapView treeMapView = mock(SwingHeapTreeMapView.class);

        try {
            when(histogramView.getUiComponent()).thenReturn(new EdtHelper().callAndWait(new Callable<JPanel>() {
                @Override
                public JPanel call() throws Exception {
                    return new JPanel();
                }
            }));


            when(objectDetailsView.getUiComponent()).thenReturn(new EdtHelper().callAndWait(new Callable<JPanel>() {
                @Override
                public JPanel call() throws Exception {
                    return new JPanel();
                }
            }));


            when(treeMapView.getUiComponent()).thenReturn(new EdtHelper().callAndWait(new Callable<JPanel>() {
                @Override
                public JPanel call() throws Exception {
                    return new JPanel();
                }
            }));
        } catch (InvocationTargetException | InterruptedException e) {
            Assert.fail("Did not expect exception");
        }

        return new Triple<>(histogramView, objectDetailsView, treeMapView);
    }

}

