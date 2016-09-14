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

package com.redhat.thermostat.vm.byteman.client.swing.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Container;
import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.fest.swing.annotation.GUITest;
import org.fest.swing.core.NameMatcher;
import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.Containers;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JTextComponentFixture;
import org.fest.swing.fixture.JToggleButtonFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.redhat.thermostat.annotations.internal.CacioTest;
import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.client.swing.components.ActionToggleButton;
import com.redhat.thermostat.client.swing.components.Icon;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.vm.byteman.client.swing.internal.VmBytemanView.BytemanInjectState;
import com.redhat.thermostat.vm.byteman.common.BytemanMetric;
import com.redhat.thermostat.vm.byteman.common.VmBytemanDAO;

import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;

@Category(CacioTest.class)
@RunWith(CacioFESTRunner.class)
public class SwingVmBytemanViewTest {

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    private SwingVmBytemanView view;
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
                view = new SwingVmBytemanView();
            }
        });

        frame = Containers.frameFixtureFor((Container) view.getUiComponent());
    }

    @After
    public void tearDown() {
        frame.cleanUp();
        frame = null;
        view = null;
    }

    @GUITest
    @Test
    public void testShifterInjectAndUnloadButtons() throws InvocationTargetException, InterruptedException {
        String content = "RULE foo bar baz";
        String newContent = "RULE new rule";
        String mainClass = "foo-main-class";

        // Initial set-up
        assertEquals(true, view.isInjectButtonEnabled());
        assertEquals(false, view.isUnloadButtonEnabled());
        assertEquals(t.localize(LocaleResources.NO_RULES_LOADED).getContents(), view.getInjectedRuleContent());
        assertEquals(t.localize(LocaleResources.NO_RULES_LOADED).getContents(), view.getUnloadedRuleContent());

        testInjectRule(content);
        testPreservingRuleDuringActionEvent(content, newContent);
        testUnloadRule(newContent);
        testGenerateRule(mainClass);
    }

    private void testInjectRule(String content) throws InvocationTargetException, InterruptedException {
        view.setUnloadedRuleContent(content);
        assertEquals(content, view.getUnloadedRuleContent());
        setInjectStateInEDT(VmBytemanView.BytemanInjectState.INJECTED);
        assertEquals(content, view.getInjectedRuleContent());
        assertEquals(false, view.isInjectButtonEnabled());
        assertEquals(true, view.isUnloadButtonEnabled());
    }

    private void testPreservingRuleDuringActionEvent(String oldContent, String newContent) throws InvocationTargetException, InterruptedException {
        view.setUnloadedRuleContent(newContent);
        ActionEvent<VmBytemanView.TabbedPaneContentAction> event = new ActionEvent<>(this, VmBytemanView.TabbedPaneContentAction.RULES_CHANGED);
        event.setPayload(oldContent);
        view.contentChanged(event);
        assertEquals(newContent, view.getUnloadedRuleContent());
        assertEquals(oldContent, view.getInjectedRuleContent());
    }

    private void testUnloadRule(String content) throws InvocationTargetException, InterruptedException {
        setInjectStateInEDT(VmBytemanView.BytemanInjectState.UNLOADED);
        ActionEvent event = new ActionEvent<>(this, VmBytemanView.TabbedPaneContentAction.RULES_CHANGED);
        event.setPayload(t.localize(LocaleResources.NO_RULES_LOADED).getContents());
        view.contentChanged(event);
        assertEquals(t.localize(LocaleResources.NO_RULES_LOADED).getContents(), view.getInjectedRuleContent());
        assertEquals(content, view.getUnloadedRuleContent());
        assertEquals(true, view.isInjectButtonEnabled());
        assertEquals(false, view.isUnloadButtonEnabled());
    }

    private void testGenerateRule(String mainClass) throws InvocationTargetException, InterruptedException {
        VmBytemanInformationController controller = createController();
        String template = controller.generateTemplateForVM(mainClass);
        ActionEvent event = new ActionEvent<>(this, VmBytemanView.TabbedPaneContentAction.RULES_CHANGED);
        event.setPayload(template);
        view.enableGenerateRuleToggle();
        view.contentChanged(event);
        assertEquals(template, view.getUnloadedRuleContent());
    }


    @GUITest
    @Test
    public void testInjectButton() throws InvocationTargetException, InterruptedException {
        frame.show();

        JToggleButtonFixture toggleButtonFixture = frame.toggleButton(SwingVmBytemanView.TOGGLE_BUTTON_NAME);
        final ActionToggleButton toggleButton = (ActionToggleButton) toggleButtonFixture.component();

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                toggleButton.toggleText(true);
                view.setViewControlsEnabled(false);
                view.setInjectState(BytemanInjectState.INJECTED);
            }
        });

        checkButtonState(VmBytemanView.BytemanInjectState.DISABLED, toggleButton);
        assertEquals(t.localize(LocaleResources.INJECT_RULE).getContents(),
                toggleButtonFixture.text());

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                view.setViewControlsEnabled(true);
                view.setInjectState(VmBytemanView.BytemanInjectState.UNLOADING);
            }
        });
        checkButtonState(VmBytemanView.BytemanInjectState.UNLOADING, toggleButton);
        assertEquals(t.localize(LocaleResources.UNLOAD_RULE).getContents(), toggleButtonFixture.text());
        assertEquals(view.isInjectButtonEnabled(), false);
        assertEquals(view.isUnloadButtonEnabled(), true);

        setInjectStateInEDT(VmBytemanView.BytemanInjectState.INJECTED);
        checkButtonState(VmBytemanView.BytemanInjectState.INJECTED, toggleButton);
        assertEquals(view.isInjectButtonEnabled(), false);
        assertEquals(view.isUnloadButtonEnabled(), true);
        assertEquals(t.localize(LocaleResources.UNLOAD_RULE).getContents(), toggleButtonFixture.text());

        setInjectStateInEDT(VmBytemanView.BytemanInjectState.UNLOADED);
        checkButtonState(VmBytemanView.BytemanInjectState.UNLOADED, toggleButton);
        assertEquals(view.isInjectButtonEnabled(), true);
        assertEquals(view.isUnloadButtonEnabled(), false);
        assertEquals(t.localize(LocaleResources.INJECT_RULE).getContents(), toggleButtonFixture.text());

        setInjectStateInEDT(VmBytemanView.BytemanInjectState.DISABLED);
        checkButtonState(VmBytemanView.BytemanInjectState.DISABLED, toggleButton);
        assertEquals(t.localize(LocaleResources.INJECT_RULE).getContents(), toggleButtonFixture.text());

        setInjectStateInEDT(VmBytemanView.BytemanInjectState.INJECTING);
        checkButtonState(VmBytemanView.BytemanInjectState.INJECTING, toggleButton);
        assertEquals(t.localize(LocaleResources.INJECT_RULE).getContents(), toggleButtonFixture.text());
        assertEquals(view.isInjectButtonEnabled(), true);
        assertEquals(view.isUnloadButtonEnabled(), false);
    }

    @GUITest
    @Test
    public void testGetRuleContent() throws InvocationTargetException, InterruptedException {
        String ruleContent = "";
        setRuleContentInEDT(ruleContent);

        String actual = view.getUnloadedRuleContent();
        assertEquals(ruleContent, actual);

        ruleContent = "foo\nbar\nbaz";
        setRuleContentInEDT(ruleContent);
        actual = view.getUnloadedRuleContent();
        assertEquals(ruleContent, actual);
    }

    @GUITest
    @Test
    public void testContentChangedMetrics() {
        String content = "{ \"foo\": \"bar\" }";
        String marker = "marker";
        long timestamp = 1_440_000_000_000L;
        DateFormat metricsDateFormat = SwingVmBytemanView.metricsDateFormat;
        String timestring = metricsDateFormat.format(new Date(timestamp));
        BytemanMetric m = new BytemanMetric();
        m.setData(content);
        m.setMarker(marker);
        m.setTimeStamp(timestamp);

        ActionEvent<VmBytemanView.TabbedPaneContentAction> event = new ActionEvent<>(this, VmBytemanView.TabbedPaneContentAction.METRICS_CHANGED);
        event.setPayload(Arrays.asList(m));
        view.contentChanged(event);
        verifyMetricsTextEquals(timestring + ": " + marker + " " + content + "\n");

        // Do the same with an empty metrics list
        event = new ActionEvent<>(this, VmBytemanView.TabbedPaneContentAction.METRICS_CHANGED);
        event.setPayload(Collections.emptyList());
        view.contentChanged(event);
        verifyMetricsTextEquals(SwingVmBytemanView.NO_METRICS_AVAILABLE + "\n");
    }

    @GUITest
    @Test
    public void testContentChangedRules() throws InvocationTargetException, InterruptedException {

        String content = "RULE foo bar baz";

        // Unloaded rules shouldn't be overwritten if they already have content in them
        String unloadedRule = "RULE baz bar foo";
        view.setUnloadedRuleContent(unloadedRule);
        assertEquals(view.getUnloadedRuleContent(), unloadedRule);
        ActionEvent<VmBytemanView.TabbedPaneContentAction> event = new ActionEvent<>(this, VmBytemanView.TabbedPaneContentAction.RULES_CHANGED);
        event.setPayload(content);
        view.contentChanged(event);
        verifyUnloadedRulesTextEquals(unloadedRule);

        // Do the same, now setting empty
        content = "";
        view.setUnloadedRuleContent(content);
        event = new ActionEvent<>(this, VmBytemanView.TabbedPaneContentAction.RULES_CHANGED);
        event.setPayload(content);
        view.contentChanged(event);
        verifyUnloadedRulesTextEquals(content);
    }

    private void verifyUnloadedRulesTextEquals(String expected) {
        final JTextComponent text = getJTextAreaWithName(SwingVmBytemanView.RULES_UNLOADED_TEXT_NAME);
        verifyEquals(text, expected);
    }

    private void verifyMetricsTextEquals(String expected) {
        final JTextComponent text = getJTextAreaWithName(SwingVmBytemanView.METRICS_TEXT_NAME);
        verifyEquals(text, expected);
    }

    private void verifyEquals(final JTextComponent text, final String expected) {
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                assertEquals(expected, text.getText());
            }
        });
    }

    private void setRuleContentInEDT(final String ruleContent) {
        final JTextComponent text = getJTextAreaWithName(SwingVmBytemanView.RULES_UNLOADED_TEXT_NAME);
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                text.setText(ruleContent);
            }
        });
        
    }

    private JTextComponent getJTextAreaWithName(String name) {
        NameMatcher textMatcher = new NameMatcher(name, JTextArea.class);
        JTextComponentFixture textFixture = new JTextComponentFixture(frame.robot, (JTextArea)frame.robot.finder().find(frame.component(), textMatcher));
        return (JTextComponent) textFixture.component();
    }

    private void checkButtonState(final BytemanInjectState state, final ActionToggleButton toggleButton)
            throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                ActionToggleButton reference = new ActionToggleButton(new Icon());
                reference.setToggleActionState(state);
                assertEquals(reference.isEnabled(), toggleButton.isEnabled());
                assertEquals(reference.isSelected(), toggleButton.isSelected());
            }
        });

    }

    private void setInjectStateInEDT(final VmBytemanView.BytemanInjectState state) {
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                view.setInjectState(state);
            }
        });
    }

    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("VmBytemanView");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //Set up the content pane.
        Container cont = frame.getContentPane();
        SwingVmBytemanView view = new SwingVmBytemanView();
        cont.add(view.getUiComponent());
        frame.setPreferredSize(new Dimension(500, 500));
        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    // For basic, quick and dirty UI testing
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }

    private VmBytemanInformationController createController() {
        VmRef ref = mock(VmRef.class);
        when(ref.getVmId()).thenReturn("some-vm-id");
        when(ref.getHostRef()).thenReturn(new HostRef("some-agent-id", "some-host-name"));
        AgentInfoDAO agentInfoDao = mock(AgentInfoDAO.class);
        AgentInformation agentInfo = mock(AgentInformation.class);
        when(agentInfo.isAlive()).thenReturn(true);
        when(agentInfoDao.getAgentInformation(any(AgentId.class))).thenReturn(agentInfo);
        VmInfoDAO vmInfoDao = mock(VmInfoDAO.class);
        VmInfo vmInfo = mock(VmInfo.class);
        when(vmInfo.isAlive(agentInfo)).thenReturn(VmInfo.AliveStatus.RUNNING);
        when(vmInfoDao.getVmInfo(any(VmId.class))).thenReturn(vmInfo);
        when(vmInfoDao.getVmInfo(any(VmRef.class))).thenReturn(vmInfo);
        VmBytemanDAO vmBytemanDao = mock(VmBytemanDAO.class);
        RequestQueue requestQueue = mock(RequestQueue.class);
        return new VmBytemanInformationController(view, ref, agentInfoDao, vmInfoDao, vmBytemanDao, requestQueue) {

            @Override
            void waitWithTimeOut(CountDownLatch latch) {
                // nothing, return immediately for tests
            }
        };
    }

}
