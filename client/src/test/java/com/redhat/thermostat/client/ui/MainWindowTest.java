package com.redhat.thermostat.client.ui;

import static org.mockito.Mockito.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import org.apache.commons.lang3.ObjectUtils;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JTextComponentFixture;
import org.hamcrest.Description;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;

import com.redhat.thermostat.client.ChangeableText;
import com.redhat.thermostat.client.MainWindowFacade;
import com.redhat.thermostat.client.SummaryPanelFacade;
import com.redhat.thermostat.client.UiFacadeFactory;

public class MainWindowTest {

    private static class PropertyChangeEventMatcher extends ArgumentMatcher<PropertyChangeEvent> {

        private PropertyChangeEvent event;

        private PropertyChangeEventMatcher(PropertyChangeEvent ev) {
            event = ev;
        }

        @Override
        public boolean matches(Object argument) {
            PropertyChangeEvent other = (PropertyChangeEvent) argument;
            return event.getSource() == other.getSource()
                    && ObjectUtils.equals(event.getPropertyName(), other.getPropertyName())
                    && ObjectUtils.equals(event.getNewValue(), other.getNewValue())
                    && ObjectUtils.equals(event.getOldValue(), other.getOldValue());
        }

        @Override
        public void describeTo(Description description) {
            super.describeTo(description);
            description.appendText(event.getSource() + ", " + event.getPropertyName() + ", " + event.getOldValue() + ", " + event.getNewValue());
        }
    }

    @Test
    public void testHostVMTreeFilterPropertySupport() {
        MainWindowFacade mainWindowFacade = mock(MainWindowFacade.class);
        TreeNode root = new DefaultMutableTreeNode();
        TreeModel treeModel = new DefaultTreeModel(root);
        when(mainWindowFacade.getHostVmTree()).thenReturn(treeModel);

        SummaryPanelFacade summaryPanelFacade = mock(SummaryPanelFacade.class);
        when(summaryPanelFacade.getTotalConnectedAgents()).thenReturn(new ChangeableText("totalConnectedAgents"));
        when(summaryPanelFacade.getTotalConnectedVms()).thenReturn(new ChangeableText("connectedVms"));

        UiFacadeFactory uiFacadeFactory = mock(UiFacadeFactory.class);
        when(uiFacadeFactory.getMainWindow()).thenReturn(mainWindowFacade);
        when(uiFacadeFactory.getSummaryPanel()).thenReturn(summaryPanelFacade);

        MainWindow window = new MainWindow(uiFacadeFactory);
        PropertyChangeListener l = mock(PropertyChangeListener.class);
        window.addViewPropertyListener(l);

        FrameFixture frameFixture = new FrameFixture(window);
        frameFixture.show();
        JTextComponentFixture hostVMTreeFilterField = frameFixture.textBox("hostVMTreeFilter");
        hostVMTreeFilterField.enterText("test");

        InOrder inOrder = inOrder(l);
        inOrder.verify(l).propertyChange(argThat(new PropertyChangeEventMatcher(new PropertyChangeEvent(window, MainWindow.HOST_VM_TREE_FILTER_PROPERTY, null, "t"))));
        inOrder.verify(l).propertyChange(argThat(new PropertyChangeEventMatcher(new PropertyChangeEvent(window, MainWindow.HOST_VM_TREE_FILTER_PROPERTY, null, "te"))));
        inOrder.verify(l).propertyChange(argThat(new PropertyChangeEventMatcher(new PropertyChangeEvent(window, MainWindow.HOST_VM_TREE_FILTER_PROPERTY, null, "tes"))));
        inOrder.verify(l).propertyChange(argThat(new PropertyChangeEventMatcher(new PropertyChangeEvent(window, MainWindow.HOST_VM_TREE_FILTER_PROPERTY, null, "test"))));
    }

}
