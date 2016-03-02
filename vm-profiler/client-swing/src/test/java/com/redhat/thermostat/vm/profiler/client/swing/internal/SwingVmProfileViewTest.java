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

package com.redhat.thermostat.vm.profiler.client.swing.internal;

import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;

import org.fest.swing.annotation.GUITest;
import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.Containers;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JLabelFixture;
import org.fest.swing.fixture.JListFixture;
import org.fest.swing.fixture.JScrollPaneFixture;
import org.fest.swing.fixture.JTabbedPaneFixture;
import org.fest.swing.fixture.JToggleButtonFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.redhat.thermostat.annotations.internal.CacioTest;
import com.redhat.thermostat.client.swing.components.ActionToggleButton;
import com.redhat.thermostat.client.swing.components.Icon;
import com.redhat.thermostat.client.swing.components.ThermostatTable;
import com.redhat.thermostat.client.swing.components.ThermostatTableRenderer;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.utils.MethodDescriptorConverter;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.vm.profiler.client.core.ProfilingResult;
import com.redhat.thermostat.vm.profiler.client.core.ProfilingResult.MethodInfo;

@Category(CacioTest.class)
@RunWith(CacioFESTRunner.class)
public class SwingVmProfileViewTest {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private PrintStream stderr;

    private SwingVmProfileView view;
    private FrameFixture frame;

    @BeforeClass
    public static void setUpOnce() {
        FailOnThreadViolationRepaintManager.install();
    }

    @Before
    public void setUp() {
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                view = new SwingVmProfileView();
            }
        });

        frame = Containers.frameFixtureFor((Container) view.getUiComponent());
        stderr = System.err;
    }

    @After
    public void tearDown() {
        System.setErr(stderr);
        frame.cleanUp();
        frame = null;
        view = null;
    }

    @GUITest
    @Test
    public void testSetProfilingState() throws InvocationTargetException, InterruptedException {
        frame.show();

        JLabelFixture currentStatusLabelFixture = frame.label("CURRENT_STATUS_LABEL");
        JToggleButtonFixture toggleButtonFixture = frame.toggleButton("TOGGLE_PROFILING_BUTTON");
        final ActionToggleButton toggleButton = (ActionToggleButton) toggleButtonFixture.component();

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                toggleButton.toggleText(true);
                view.setViewControlsEnabled(false);
                view.setProfilingState(VmProfileView.ProfilingState.STARTED);
            }
        });

        checkButtonState(VmProfileView.ProfilingState.DISABLED, toggleButton);
        assertEquals(translator.localize(LocaleResources.PROFILER_CURRENT_STATUS_DEAD).getContents(),
                currentStatusLabelFixture.text());
        assertEquals(translator.localize(LocaleResources.START_PROFILING).getContents(),
                toggleButtonFixture.text());

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                view.setViewControlsEnabled(true);
                view.setProfilingState(VmProfileView.ProfilingState.STOPPING);
            }
        });
        checkButtonState(VmProfileView.ProfilingState.STOPPING, toggleButton);
        verifyActive(currentStatusLabelFixture, toggleButtonFixture);

        setProfilingStateInEDT(VmProfileView.ProfilingState.STARTED);
        checkButtonState(VmProfileView.ProfilingState.STARTED, toggleButton);
        verifyActive(currentStatusLabelFixture, toggleButtonFixture);

        setProfilingStateInEDT(VmProfileView.ProfilingState.STOPPED);
        checkButtonState(VmProfileView.ProfilingState.STOPPED, toggleButton);
        verifyInactive(currentStatusLabelFixture, toggleButtonFixture);

        setProfilingStateInEDT(VmProfileView.ProfilingState.DISABLED);
        checkButtonState(VmProfileView.ProfilingState.DISABLED, toggleButton);
        verifyInactive(currentStatusLabelFixture, toggleButtonFixture);

        setProfilingStateInEDT(VmProfileView.ProfilingState.STARTING);
        checkButtonState(VmProfileView.ProfilingState.STARTING, toggleButton);
        verifyInactive(currentStatusLabelFixture, toggleButtonFixture);
    }

    private void setProfilingStateInEDT(final VmProfileView.ProfilingState state) {
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                view.setProfilingState(state);
            }
        });
    }

    private void checkButtonState(final VmProfileView.ProfilingState profilingState,
                                  final ActionToggleButton toggleButton)
            throws InterruptedException, InvocationTargetException {

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                ActionToggleButton reference = new ActionToggleButton(new Icon());
                reference.setToggleActionState(profilingState);
                assertEquals(reference.isEnabled(), toggleButton.isEnabled());
                assertEquals(reference.isSelected(), toggleButton.isSelected());
            }
        });
    }

    private void verifyActive(JLabelFixture currentStatusLabelFixture,
                              JToggleButtonFixture toggleButtonFixture) {
        assertEquals(translator.localize(LocaleResources.PROFILER_CURRENT_STATUS_ACTIVE).getContents(),
                currentStatusLabelFixture.text());
        assertEquals(translator.localize(LocaleResources.STOP_PROFILING).getContents(),
                toggleButtonFixture.text());
    }

    private void verifyInactive(JLabelFixture currentStatusLabelFixture,
                                JToggleButtonFixture toggleButtonFixture) {
        assertEquals(translator.localize(LocaleResources.PROFILER_CURRENT_STATUS_INACTIVE).getContents(),
                currentStatusLabelFixture.text());
        assertEquals(translator.localize(LocaleResources.START_PROFILING).getContents(),
                toggleButtonFixture.text());
    }

    @GUITest
    @Test
    public void testGetSelectedProfileWithoutSelection() throws InvocationTargetException,
            InterruptedException {

        frame.show();

        JListFixture profileJList = frame.list("PROFILE_LIST");
        profileJList.clearSelection();

        ByteArrayOutputStream divertedErr = new ByteArrayOutputStream();
        System.setErr(new PrintStream(divertedErr));

        view.getSelectedProfile();

        assertTrue(divertedErr.toString().contains(InvocationTargetException.class.getSimpleName()));
    }

    @GUITest
    @Test
    public void testGetSelectedProfileWithSelection() throws InvocationTargetException,
            InterruptedException {

        frame.show();

        final JListFixture profileJList = frame.list("PROFILE_LIST");
        final List<VmProfileView.Profile> availableRuns = new ArrayList<>();
        availableRuns.add(new VmProfileView.Profile("profile1", 1000, 1000));
        view.setAvailableProfilingRuns(availableRuns);

        profileJList.clickItem(0);
        assertEquals(availableRuns.get(0), view.getSelectedProfile());
    }

    @GUITest
    @Test
    public void testSetProfilingDetailDataWithNoResults() throws InvocationTargetException,
            InterruptedException {

        frame.show();

        final JScrollPaneFixture scrollPane = frame.scrollPane("METHOD_TABLE");

        view.setProfilingDetailData(new ProfilingResult(new ArrayList<MethodInfo>()));

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                TableModel model =
                        ((ThermostatTable) scrollPane.component().getViewport().getView()).getModel();
                assertEquals(1, model.getRowCount());
                assertEquals(translator.localize(LocaleResources.PROFILER_NO_RESULTS).getContents(),
                        model.getValueAt(0, 0));
            }
        });
    }

    @GUITest
    @Test
    public void testSetProfilingDetailDataWithResults() throws InvocationTargetException,
            InterruptedException {

        frame.show();

        final JScrollPaneFixture scrollPane = frame.scrollPane("METHOD_TABLE");

        List<ProfilingResult.MethodInfo> data = new ArrayList<>();
        data.add(new ProfilingResult.MethodInfo(new MethodDescriptorConverter.MethodDeclaration(
                "foo", Arrays.asList("int"), "int"), 700, 70));
        data.add(new ProfilingResult.MethodInfo(new MethodDescriptorConverter.MethodDeclaration(
                "bar", Arrays.asList("double"), "double"), 300, 30));
        final ProfilingResult result = new ProfilingResult(data);

        view.setProfilingDetailData(result);

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                TableModel model =
                        ((ThermostatTable) scrollPane.component().getViewport().getView()).getModel();
                assertEquals(2, model.getRowCount());

                ArrayList<String> methodNames = new ArrayList<>();
                methodNames.add(model.getValueAt(0, 0).toString());
                methodNames.add(model.getValueAt(1, 0).toString());
                assertTrue(methodNames.contains("int foo(int)"));
                assertTrue(methodNames.contains("double bar(double)"));
            }
        });
    }


    @GUITest
    @Test
    public void testMethodsWithLargestExecutionTimeAppearFirst() throws InvocationTargetException, InterruptedException {

        frame.show();

        final JScrollPaneFixture scrollPane = frame.scrollPane("METHOD_TABLE");

        List<ProfilingResult.MethodInfo> data = new ArrayList<>();
        data.add(new ProfilingResult.MethodInfo(new MethodDescriptorConverter.MethodDeclaration(
                "foo", Arrays.asList("int"), "int"), 10000, 70));
        data.add(new ProfilingResult.MethodInfo(new MethodDescriptorConverter.MethodDeclaration(
                "bar", Arrays.asList("int"), "double"), 1000, 30));
        data.add(new ProfilingResult.MethodInfo(new MethodDescriptorConverter.MethodDeclaration(
                "baz", Arrays.asList("double"), "double"), 100, 30));
        final ProfilingResult result = new ProfilingResult(data);

        view.setProfilingDetailData(result);

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                TableModel model =
                        ((ThermostatTable) scrollPane.component().getViewport().getView()).getModel();
                assertEquals(3, model.getRowCount());

                assertEquals("int foo(int)", model.getValueAt(0, 0).toString());
                assertEquals("double bar(int)", model.getValueAt(1, 0).toString());
                assertEquals("double baz(double)", model.getValueAt(2, 0).toString());
            }
        });
    }

    @GUITest
    @Test
    public void testTabAddingAndSwitching() throws InvocationTargetException, InterruptedException {
        frame.show();
        final JTabbedPaneFixture tabPaneFixture = frame.tabbedPane();

        view.setTabbedPaneActionListener(mock(ActionListener.class));

        view.setProfilingDetailData(new ProfilingResult(new ArrayList<MethodInfo>()));
        Component testComp = mock(Component.class);
        final String testCompName = "testComp";
        when(testComp.getName()).thenReturn(testCompName);
        final LocalizedString testTabName = new LocalizedString("testTab");
        view.addTabToTabbedPane(testTabName, testComp);
        final LocalizedString tableTabName =
                translator.localize(LocaleResources.PROFILER_RESULTS_TABLE);
        tabPaneFixture.requireTabTitles(tableTabName.getContents(), testTabName.getContents());

        final Component selected1 = tabPaneFixture.selectedComponent();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                assertNotNull(selected1);
                assertEquals("METHOD_TABLE", selected1.getName());
            }
        });

        tabPaneFixture.selectTab(testTabName.getContents());
        final Component selected2 = tabPaneFixture.selectedComponent();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                assertNotNull(selected2);
                assertEquals(testCompName, selected2.getName());
            }
        });

        tabPaneFixture.selectTab(tableTabName.getContents());
        final Component selected3 = tabPaneFixture.selectedComponent();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                assertNotNull(selected3);
                assertEquals("METHOD_TABLE", selected3.getName());
            }
        });
    }

    @Test
    public void testProfileItemRendererFailsWithNonProfileValue() {
        SwingVmProfileView.ProfileItemRenderer renderer = new SwingVmProfileView.ProfileItemRenderer();

        String value = "value";
        Component result = renderer.getListCellRendererComponent(
                mock(JList.class), value, 0, true, true);

        SwingVmProfileView.ProfileItemRenderer resultRenderer =
                (SwingVmProfileView.ProfileItemRenderer) result;

        assertEquals(value, resultRenderer.getText());
    }

    @Test
    public void testProfileItemRendererWithProfileValue() {
        SwingVmProfileView.ProfileItemRenderer renderer = new SwingVmProfileView.ProfileItemRenderer();

        VmProfileView.Profile value = new VmProfileView.Profile("profile", 1000, 100);
        Component result = renderer.getListCellRendererComponent(
                mock(JList.class), value, 0, true, true);

        SwingVmProfileView.ProfileItemRenderer resultRenderer =
                (SwingVmProfileView.ProfileItemRenderer) result;

        String expectedValue = translator.localize(LocaleResources.PROFILER_LIST_ITEM,
                value.name, new Date(value.startTimeStamp).toString(), new Date(value.stopTimeStamp).toString()).getContents();
        assertEquals(expectedValue, resultRenderer.getText());
    }

    @Test
    public void testSimpleTextRendererFailsWithInvalidValue() {
        SwingVmProfileView.SimpleTextRenderer renderer = new SwingVmProfileView.SimpleTextRenderer();

        testRendererWithNullValue(renderer);
        testRendererWithPlainObjectAsValue(renderer);
    }

    @Test
    public void testPlainTextMethodDeclarationRendererFailsWithInvalidValue() {
        SwingVmProfileView.PlainTextMethodDeclarationRenderer renderer =
                new SwingVmProfileView.PlainTextMethodDeclarationRenderer();

        testRendererWithNullValue(renderer);
        testRendererWithPlainObjectAsValue(renderer);
    }

    @Test
    public void testSyntaxHighlightedMethodDeclarationRendererFailsWithInvalidValue() {
        SwingVmProfileView.SyntaxHighlightedMethodDeclarationRenderer renderer =
                new SwingVmProfileView.SyntaxHighlightedMethodDeclarationRenderer();

        testRendererWithNullValue(renderer);
        testRendererWithPlainObjectAsValue(renderer);
    }

    private void testRendererWithNullValue(ThermostatTableRenderer renderer) {
        try {
            renderer.getTableCellRendererComponent(mock(JTable.class), null, true, true, 0, 0);
            fail("Error expected");
        } catch (AssertionError e) {
            assertEquals("Unexpected value", e.getMessage());
        }
    }

    private void testRendererWithPlainObjectAsValue(ThermostatTableRenderer renderer) {
        try {
            renderer.getTableCellRendererComponent(mock(JTable.class), new Object(), true, true, 0, 0);
            fail("Error expected");
        } catch (AssertionError e) {
            assertEquals("Unexpected value", e.getMessage());
        }
    }

    @Test
    public void testSyntaxHighlightedMethodDeclarationRendererWithValidValue() {
        final Color METHOD_COLOR =
                SwingVmProfileView.SyntaxHighlightedMethodDeclarationRenderer.METHOD_COLOR;
        final Color PARAMETER_COLOR =
                SwingVmProfileView.SyntaxHighlightedMethodDeclarationRenderer.PARAMETER_COLOR;
        final Color RETURN_TYPE_COLOR =
                SwingVmProfileView.SyntaxHighlightedMethodDeclarationRenderer.RETURN_TYPE_COLOR;

        SwingVmProfileView.SyntaxHighlightedMethodDeclarationRenderer renderer =
                new SwingVmProfileView.SyntaxHighlightedMethodDeclarationRenderer();

        String methodName = "foo";
        List<String> parameters = Arrays.asList("double", "String");
        String returnType = "int";
        MethodDescriptorConverter.MethodDeclaration decl =
                new MethodDescriptorConverter.MethodDeclaration(
                        methodName, parameters, returnType);

        Component result = renderer.getTableCellRendererComponent(mock(JTable.class), decl,
                true, true, 0, 0);

        SwingVmProfileView.SyntaxHighlightedMethodDeclarationRenderer resultRenderer =
                (SwingVmProfileView.SyntaxHighlightedMethodDeclarationRenderer) result;

        String highlightedReturnType = SwingVmProfileView.SyntaxHighlightedMethodDeclarationRenderer
                .htmlColorText(returnType, RETURN_TYPE_COLOR);
        String highlightedName = SwingVmProfileView.SyntaxHighlightedMethodDeclarationRenderer
                .htmlColorText(methodName, METHOD_COLOR);
        String highlightedFirstParam = SwingVmProfileView.SyntaxHighlightedMethodDeclarationRenderer
                .htmlColorText(parameters.get(0), PARAMETER_COLOR);
        String highlightedSecondParam = SwingVmProfileView.SyntaxHighlightedMethodDeclarationRenderer
                .htmlColorText(parameters.get(1), PARAMETER_COLOR);

        String expectedResult = "<html><pre>" + highlightedReturnType + " <b>" +
                highlightedName +"</b>(" + highlightedFirstParam + "," + highlightedSecondParam +
                ")</pre><html>";

        assertEquals(expectedResult, resultRenderer.getText());
    }

}
