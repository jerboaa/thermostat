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

import com.redhat.thermostat.annotations.internal.CacioTest;
import com.redhat.thermostat.client.swing.UIDefaults;
import com.redhat.thermostat.client.swing.components.ActionToggleButton;
import com.redhat.thermostat.client.swing.components.Icon;
import com.redhat.thermostat.client.swing.components.SearchField;
import com.redhat.thermostat.client.swing.components.ThermostatTableRenderer;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.internal.test.Bug;
import com.redhat.thermostat.common.utils.MethodDescriptorConverter;
import com.redhat.thermostat.common.utils.MethodDescriptorConverter.MethodDeclaration;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.vm.profiler.client.core.ProfilingResult;
import com.redhat.thermostat.vm.profiler.client.core.ProfilingResult.MethodInfo;
import com.redhat.thermostat.vm.profiler.client.swing.internal.SwingVmProfileView.PlainTextMethodDeclarationRenderer;
import com.redhat.thermostat.vm.profiler.client.swing.internal.SwingVmProfileView.ProfileItemRenderer;
import com.redhat.thermostat.vm.profiler.client.swing.internal.SwingVmProfileView.SimpleTextRenderer;
import com.redhat.thermostat.vm.profiler.client.swing.internal.SwingVmProfileView.SyntaxHighlightedMethodDeclarationRenderer;
import com.redhat.thermostat.vm.profiler.client.swing.internal.VmProfileView.ProfileAction;
import com.redhat.thermostat.vm.profiler.client.swing.internal.VmProfileView.ProfilingState;
import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;
import org.fest.swing.annotation.GUITest;
import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.exception.ComponentLookupException;
import org.fest.swing.fixture.Containers;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JLabelFixture;
import org.fest.swing.fixture.JListFixture;
import org.fest.swing.fixture.JTabbedPaneFixture;
import org.fest.swing.fixture.JTableFixture;
import org.fest.swing.fixture.JTextComponentFixture;
import org.fest.swing.fixture.JToggleButtonFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.redhat.thermostat.vm.profiler.client.swing.internal.SwingVmProfileView.CURRENT_STATUS_LABEL_NAME;
import static com.redhat.thermostat.vm.profiler.client.swing.internal.SwingVmProfileView.OVERLAY_PANEL;
import static com.redhat.thermostat.vm.profiler.client.swing.internal.SwingVmProfileView.PROFILES_LIST_NAME;
import static com.redhat.thermostat.vm.profiler.client.swing.internal.SwingVmProfileView.PROFILE_TABLE_NAME;
import static com.redhat.thermostat.vm.profiler.client.swing.internal.SwingVmProfileView.TOGGLE_BUTTON_NAME;
import static com.redhat.thermostat.vm.profiler.client.swing.internal.SwingVmProfileView.TOGGLE_PROFILE_LIST_NAME;
import static com.redhat.thermostat.vm.profiler.client.swing.internal.VmProfileView.Profile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeastOnce;

@Category(CacioTest.class)
@RunWith(CacioFESTRunner.class)
public class SwingVmProfileViewTest {

    private static final int METHOD_NAME_INDEX = 0;

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private PrintStream stderr;

    private SwingVmProfileView view;
    private FrameFixture frame;

    private UIDefaults uiDefaults;

    @BeforeClass
    public static void setUpOnce() {
        FailOnThreadViolationRepaintManager.install();
    }

    @Before
    public void setUp() {

        uiDefaults = mock(UIDefaults.class);

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                view = new SwingVmProfileView(uiDefaults);
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

    @Bug(id = "3088",
         url = "http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=3088",
         summary = "Profiling View has duplicate button icons with no tooltips\n")
    @GUITest
    @Test
    public void testSessionAndProfilingIconsNotSame() {
        frame.show();

        JToggleButtonFixture listProfiles = frame.toggleButton(TOGGLE_PROFILE_LIST_NAME);
        JToggleButtonFixture toggleProfile = frame.toggleButton(TOGGLE_BUTTON_NAME);

        assertNotSame(listProfiles.target.getIcon(), toggleProfile.target.getIcon());
    }

    @GUITest
    @Test
    public void testSetProfilingState() throws InvocationTargetException, InterruptedException {
        frame.show();

        JToggleButtonFixture toggleButtonFixture = frame.toggleButton(TOGGLE_BUTTON_NAME);
        final ActionToggleButton toggleButton = (ActionToggleButton) toggleButtonFixture.component();

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                toggleButton.toggleText(true);
                view.setViewControlsEnabled(false);
                view.setProfilingState(ProfilingState.STARTED);
            }
        });

        checkButtonState(ProfilingState.DISABLED, toggleButton);
        assertEquals(translator.localize(LocaleResources.START_PROFILING).getContents(),
                toggleButtonFixture.text());

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                view.setViewControlsEnabled(true);
                view.setProfilingState(ProfilingState.STOPPING);
            }
        });
        checkButtonState(ProfilingState.STOPPING, toggleButton);
        verifyActive(toggleButtonFixture);

        setProfilingStateInEDT(ProfilingState.STARTED);
        checkButtonState(ProfilingState.STARTED, toggleButton);
        verifyActive(toggleButtonFixture);

        setProfilingStateInEDT(ProfilingState.STOPPED);
        checkButtonState(ProfilingState.STOPPED, toggleButton);
        verifyInactive(toggleButtonFixture);

        setProfilingStateInEDT(ProfilingState.DISABLED);
        checkButtonState(ProfilingState.DISABLED, toggleButton);
        verifyInactive(toggleButtonFixture);

        setProfilingStateInEDT(ProfilingState.STARTING);
        checkButtonState(ProfilingState.STARTING, toggleButton);
        verifyInactive(toggleButtonFixture);
    }

    private void setProfilingStateInEDT(final ProfilingState state) {
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                view.setProfilingState(state);
            }
        });
    }

    private void checkButtonState(final ProfilingState profilingState,
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

    private void verifyActive(JToggleButtonFixture toggleButtonFixture) {
        assertEquals(translator.localize(LocaleResources.STOP_PROFILING).getContents(),
                toggleButtonFixture.text());
    }

    private void verifyInactive(JToggleButtonFixture toggleButtonFixture) {
        assertEquals(translator.localize(LocaleResources.START_PROFILING).getContents(),
                toggleButtonFixture.text());
    }

    @GUITest
    @Test
    public void testGetSelectedProfileFromList() throws InvocationTargetException,
            InterruptedException
    {

        Profile profilingResult0 = new Profile("test0", 0, 1);
        Profile profilingResult1 = new Profile("test1", 1, 2);

        final List<Profile> profileList = new ArrayList<>();
        profileList.add(profilingResult0);
        profileList.add(profilingResult1);

        view.addProfileActionListener(new ActionListener<ProfileAction>() {
            @Override
            public void actionPerformed(com.redhat.thermostat.common.ActionEvent<ProfileAction> actionEvent) {
                switch (actionEvent.getActionId()) {

                    case DISPLAY_PROFILING_SESSIONS:
                        view.setAvailableProfilingRuns(profileList);
                        view.setDisplayProfilingRuns(true);
                        break;
                    default:
                        break;
                }
            }
        });

        frame.show(new Dimension(800, 800));

        JToggleButtonFixture toggle = frame.toggleButton(TOGGLE_PROFILE_LIST_NAME);
        toggle.click();

        JListFixture profileJList = frame.panel(OVERLAY_PANEL).list(PROFILES_LIST_NAME);
        profileJList.clickItem(1);

        String name = view.getSelectedProfile().name;
        assertEquals("test1", name);
    }

    @GUITest
    @Test
    public void testSetProfilingDetailDataWithNoResults() throws InvocationTargetException,
            InterruptedException {

        frame.show();

        view.setProfilingDetailData(new ProfilingResult(new ArrayList<MethodInfo>()));

        final JTableFixture table = frame.table(PROFILE_TABLE_NAME);
        String[][] contents = table.contents();

        assertEquals(1, contents.length);
        String[] firstRow = contents[0];
        assertEquals(3, firstRow.length);
        assertEquals(translator.localize(LocaleResources.PROFILER_NO_RESULTS).getContents(), firstRow[METHOD_NAME_INDEX]);
    }

    @GUITest
    @Test
    public void testSetProfilingDetailDataWithResults() throws InvocationTargetException,
            InterruptedException {

        frame.show();

        List<ProfilingResult.MethodInfo> data = new ArrayList<>();
        data.add(new ProfilingResult.MethodInfo(new MethodDescriptorConverter.MethodDeclaration(
                "foo", Arrays.asList("int"), "int"), 700, 70));
        data.add(new ProfilingResult.MethodInfo(new MethodDescriptorConverter.MethodDeclaration(
                "bar", Arrays.asList("double"), "double"), 300, 30));
        final ProfilingResult result = new ProfilingResult(data);

        view.setProfilingDetailData(result);

        final JTableFixture table = frame.table(PROFILE_TABLE_NAME);

        String[][] contents = table.contents();
        assertEquals(2, contents.length);

        ArrayList<String> methodNames = new ArrayList<>();
        methodNames.add(stripHtml(contents[0][METHOD_NAME_INDEX].toString()));
        methodNames.add(stripHtml(contents[1][METHOD_NAME_INDEX].toString()));
        assertTrue(methodNames.contains("int foo(int)"));
        assertTrue(methodNames.contains("double bar(double)"));
    }

    @GUITest
    @Test
    public void testMethodsWithLargestExecutionTimeAppearFirst() throws InvocationTargetException, InterruptedException {

        frame.show();

        List<ProfilingResult.MethodInfo> data = new ArrayList<>();
        data.add(new ProfilingResult.MethodInfo(new MethodDescriptorConverter.MethodDeclaration(
                "foo", Arrays.asList("int"), "int"), 10000, 70));
        data.add(new ProfilingResult.MethodInfo(new MethodDescriptorConverter.MethodDeclaration(
                "bar", Arrays.asList("int"), "double"), 1000, 30));
        data.add(new ProfilingResult.MethodInfo(new MethodDescriptorConverter.MethodDeclaration(
                "baz", Arrays.asList("double"), "double"), 100, 30));
        final ProfilingResult result = new ProfilingResult(data);

        view.setProfilingDetailData(result);

        final JTableFixture table = frame.table(PROFILE_TABLE_NAME);

        String[][] contents = table.contents();
        assertEquals(3, contents.length);

        assertEquals("int foo(int)", stripHtml(contents[0][METHOD_NAME_INDEX].toString()));
        assertEquals("double bar(int)", stripHtml(contents[1][METHOD_NAME_INDEX].toString()));
        assertEquals("double baz(double)", stripHtml(contents[2][METHOD_NAME_INDEX].toString()));
    }

    @GUITest
    @Test
    public void testFilterEvent() throws Exception {
        final String SOME_TEXT = "foo bar";

        @SuppressWarnings("unchecked")
        ActionListener<ProfileAction> listener = mock(ActionListener.class);
        view.addProfileActionListener(listener);
        frame.show();

        JTextComponentFixture searchBox = frame.textBox(SearchField.VIEW_NAME);
        searchBox.enterText(SOME_TEXT);

        ArgumentCaptor<ActionEvent> captor = ArgumentCaptor.forClass(ActionEvent.class);
        verify(listener, atLeastOnce()).actionPerformed(captor.capture());
        assertEquals(ProfileAction.PROFILE_TABLE_FILTER_CHANGED, captor.getValue().getActionId());

        assertEquals(SOME_TEXT, view.getProfilingDataFilter());
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

        // visible by default:
        frame.table(PROFILE_TABLE_NAME).requireVisible();

        tabPaneFixture.selectTab(testTabName.getContents());
        final Component selected2 = tabPaneFixture.selectedComponent();
        try {
            frame.table(PROFILE_TABLE_NAME);
            fail();
        } catch (ComponentLookupException e) {
            // pass: not visible
        }

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                assertNotNull(selected2);
                assertEquals(testCompName, selected2.getName());
            }
        });

        tabPaneFixture.selectTab(tableTabName.getContents());
        frame.table(PROFILE_TABLE_NAME).requireVisible();

    }

    @Test
    public void testSpinnerShownWhileProfiling() {

        // This test seems a bit weird, but this is because of the invokeLater
        // called inside the setProfilingState method, that means we can't
        // check right away if the view state is updated, we must wait that
        // the event is actually delivered to the event queue; we can, however
        // schedule the next state change in the same event used to check the
        // previous state

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                view.setProfilingState(ProfilingState.STARTING);
            }
        });

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                assertTrue(view.getSpinner().isSpinnerEnabled());
                view.setProfilingState(ProfilingState.STARTED);
            }
        });

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                assertTrue(view.getSpinner().isSpinnerEnabled());
                view.setProfilingState(ProfilingState.STOPPING);
            }
        });


        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                assertTrue(view.getSpinner().isSpinnerEnabled());
                view.setProfilingState(ProfilingState.STOPPED);
            }
        });

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                assertFalse(view.getSpinner().isSpinnerEnabled());
            }
        });
    }

    @Test
    public void testProfileItemRendererFailsWithNonProfileValue() {
        ProfileItemRenderer renderer = new ProfileItemRenderer(uiDefaults);

        String value = "value";
        Component result = renderer.getListCellRendererComponent(
                mock(JList.class), value, 0, true, true);

        ProfileItemRenderer resultRenderer =
                (ProfileItemRenderer) result;

        assertEquals(value, resultRenderer.getText());
    }

    @Test
    public void testProfileItemRendererWithProfileValue() {
        final ProfileItemRenderer renderer = new ProfileItemRenderer(uiDefaults);

        final Profile value = new Profile("profile", 1000, 100);

        final Component [] result = new Component[1];
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                result[0] = renderer.getListCellRendererComponent(
                        mock(JList.class), value, 0, true, true);
            }
        });


        JPanel resultRenderer = (JPanel) result[0];

        String expectedValue = translator.localize(LocaleResources.PROFILER_LIST_ITEM,
                value.name, new Date(value.startTimeStamp).toString(), new Date(value.stopTimeStamp).toString()).getContents();
        assertEquals(expectedValue, resultRenderer.getName());
    }

    @Test
    public void testSimpleTextRendererFailsWithInvalidValue() {
        SimpleTextRenderer renderer = new SimpleTextRenderer();

        testRendererWithNullValue(renderer);
        testRendererWithPlainObjectAsValue(renderer);
    }

    @Test
    public void testPlainTextMethodDeclarationRendererFailsWithInvalidValue() {
        PlainTextMethodDeclarationRenderer renderer =
                new PlainTextMethodDeclarationRenderer();

        testRendererWithNullValue(renderer);
        testRendererWithPlainObjectAsValue(renderer);
    }

    @Test
    public void testSyntaxHighlightedMethodDeclarationRendererFailsWithInvalidValue() {
        SyntaxHighlightedMethodDeclarationRenderer renderer =
                new SyntaxHighlightedMethodDeclarationRenderer();

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
                SyntaxHighlightedMethodDeclarationRenderer.METHOD_COLOR;
        final Color PARAMETER_COLOR =
                SyntaxHighlightedMethodDeclarationRenderer.PARAMETER_COLOR;
        final Color RETURN_TYPE_COLOR =
                SyntaxHighlightedMethodDeclarationRenderer.RETURN_TYPE_COLOR;

        SyntaxHighlightedMethodDeclarationRenderer renderer =
                new SyntaxHighlightedMethodDeclarationRenderer();

        String methodName = "foo";
        List<String> parameters = Arrays.asList("double", "String");
        String returnType = "int";
        MethodDescriptorConverter.MethodDeclaration decl =
                new MethodDescriptorConverter.MethodDeclaration(
                        methodName, parameters, returnType);

        Component result = renderer.getTableCellRendererComponent(mock(JTable.class), decl,
                true, true, 0, 0);

        SyntaxHighlightedMethodDeclarationRenderer resultRenderer =
                (SyntaxHighlightedMethodDeclarationRenderer) result;

        String highlightedReturnType = SyntaxHighlightedMethodDeclarationRenderer
                .htmlColorText(returnType, RETURN_TYPE_COLOR);
        String highlightedName = SyntaxHighlightedMethodDeclarationRenderer
                .htmlColorText(methodName, METHOD_COLOR);
        String highlightedFirstParam = SyntaxHighlightedMethodDeclarationRenderer
                .htmlColorText(parameters.get(0), PARAMETER_COLOR);
        String highlightedSecondParam = SyntaxHighlightedMethodDeclarationRenderer
                .htmlColorText(parameters.get(1), PARAMETER_COLOR);

        String expectedResult = "<html><pre>" + highlightedReturnType + " <b>" +
                highlightedName +"</b>(" + highlightedFirstParam + "," + highlightedSecondParam +
                ")</pre><html>";

        assertEquals(expectedResult, resultRenderer.getText());
    }


    private String stripHtml(String string) {
        return string.replaceAll("<[^>]+>", "");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                UIDefaults uiDefaults = mock(UIDefaults.class);

                JFrame window = new JFrame();
                SwingVmProfileView view = new SwingVmProfileView(uiDefaults);
                window.add(view.getUiComponent());
                window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                window.pack();
                window.setVisible(true);

                List<MethodInfo> data = new ArrayList<>();
                data.add(new MethodInfo(new MethodDeclaration(
                        "foo", list("int"), "int"), 1000, 1.0));
                data.add(new MethodInfo(new MethodDeclaration(
                        "bar", list("foo.bar.Baz", "int"), "Bar"), 100000, 100));
                ProfilingResult results = new ProfilingResult(data);
                view.setProfilingDetailData(results);
            }

        });
    }

    private static List<String> list(String... args) {
        return Arrays.asList(args);
    }
}
