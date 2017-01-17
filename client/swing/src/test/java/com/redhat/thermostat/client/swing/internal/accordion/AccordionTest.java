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

package com.redhat.thermostat.client.swing.internal.accordion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;

import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;

import org.fest.swing.annotation.GUITest;
import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiQuery;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JLabelFixture;
import org.fest.swing.fixture.JPanelFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.redhat.thermostat.annotations.internal.CacioTest;
import com.redhat.thermostat.client.swing.components.EmptyIcon;

@Category(CacioTest.class)
@RunWith(CacioFESTRunner.class)
public class AccordionTest {

    private FrameFixture fixture;
    private Accordion<String, String> accordion;
    
    private static class AccordionComponentImpl implements AccordionComponent {

        private boolean selected;
        private JLabel component;
        
        public AccordionComponentImpl(final String text, final String header, final AccordionModel<String, String> model) {
            component = new JLabel(text);
            component.setName(text);
        }

        @Override
        public Component getUiComponent() {
            return component;
        }
        
        @Override
        public boolean isSelected() {
            return selected;
        }
        
        @Override
        public void setSelected(boolean selected) {
            this.selected = selected;
        }
    }
    
    private static class AccordionComponentFactoryImpl implements AccordionComponentFactory<String, String> {

        AccordionModel<String, String> model;
        
        @Override
        public TitledPane createHeader(final String header) {
            TitledPane pane = new TitledPane(header);
            pane.setName(header);
            return pane;
        }

        @Override
        public AccordionComponent createComponent(String header, final String component) {
            return new AccordionComponentImpl(component, header, model);
        }

        public void setModel(AccordionModel<String, String> model) {
            this.model = model;
        }
        
        @Override
        public void removeComponent(AccordionComponent accordionComponent,
                                    String header, String component)
        {}
        
        @Override
        public void removeHeader(TitledPane pane, String header) {}
    }
    
    @BeforeClass
    public static void setUpOnce() {
        FailOnThreadViolationRepaintManager.install();
    }
    
    @Before
    public void setUp() {
        JFrame frame = GuiActionRunner.execute(new GuiQuery<JFrame>() {
            @Override
            protected JFrame executeInEDT() throws Throwable {
                JFrame frame = new JFrame();

                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setMinimumSize(new Dimension(500, 500));
                AccordionComponentFactoryImpl impl = new AccordionComponentFactoryImpl();
                accordion = new Accordion<>(impl);
                AccordionModel<String, String> model = accordion.getModel();
                impl.setModel(model);
                frame.add(accordion);
                
                return frame;
            }
        });
        fixture = new FrameFixture(frame);
    }
    
    @After
    public void tearDown() {
        fixture.cleanUp();
        fixture = null;
    }
    
    @Category(GUITest.class)
    @GUITest
    @Test
    public void testSelectionListeners() {
        fixture.show();
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                AccordionModel<String, String> model = accordion.getModel();
                model.addHeader("Test0");
                model.addHeader("Test1");
                model.addHeader("Test2");
                
                model.addComponent("Test0", "TestComponent0");
                model.addComponent("Test0", "TestComponent1");
                
                model.addComponent("Test1", "TestComponent2");
                model.addComponent("Test1", "TestComponent3");
            }
        });
        
        JPanelFixture titlePane0 = fixture.panel("Test0");
        titlePane0.doubleClick();
        
        JPanelFixture titlePane1 = fixture.panel("Test1");
        titlePane1.doubleClick();
        
        JPanelFixture titlePane2 = fixture.panel("Test2");
        titlePane2.click();
        
        final List<ItemSelectedEvent> events = new ArrayList<>();
        
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                accordion.addAccordionItemSelectedChangeListener(new AccordionItemSelectedChangeListener() {
                    @Override
                    public void itemSelected(ItemSelectedEvent event) {
                        events.add(event);
                    }
                });                
            }
        });
        
        fixture.robot.waitForIdle();
        
        assertEquals(0, events.size());
        
        titlePane0.label("Test0_label").click();
        fixture.robot.waitForIdle();
        
        assertEquals(1, events.size());
        
        ItemSelectedEvent event = events.get(0);
        assertSame(titlePane2.target, event.getPreviousSelected());
        assertSame(titlePane0.target, event.getSelected());

        assertTrue(event.getSelected().isSelected());
        assertFalse(event.getPreviousSelected().isSelected());

        titlePane1.label("Test1_label").click();
        fixture.robot.waitForIdle();
        
        assertEquals(2, events.size());
        
        event = events.get(1);
        assertSame(titlePane0.target, event.getPreviousSelected());
        assertSame(titlePane1.target, event.getSelected());
        
        assertTrue(event.getSelected().isSelected());
        assertFalse(event.getPreviousSelected().isSelected());
        
        titlePane2.label("Test2_label").click();
        fixture.robot.waitForIdle();
        
        assertEquals(3, events.size());
        
        event = events.get(2);
        assertSame(titlePane1.target, event.getPreviousSelected());
        assertSame(titlePane2.target, event.getSelected());
        
        assertTrue(event.getSelected().isSelected());
        assertFalse(event.getPreviousSelected().isSelected());
        
        JLabelFixture component1 = titlePane0.label("TestComponent1");
        component1.click();
        fixture.robot.waitForIdle();
        
        assertEquals(4, events.size());
        event = events.get(3);
        assertSame(titlePane2.target, event.getPreviousSelected());
        assertSame(component1.target, event.getSelected().getUiComponent());
        
        // now those should both be unselected, last event clicked on
        // TestComponent1, which is a different component than 
        // its parent header Test0
        event = events.get(0);
        assertFalse(event.getSelected().isSelected());
        assertFalse(event.getPreviousSelected().isSelected());
    }
    
    @Category(GUITest.class)
    @GUITest
    @Test
    public void testComponets() {
        
        fixture.show();
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                AccordionModel<String, String> model = accordion.getModel();
                model.addHeader("Test0");
                model.addHeader("Test1");
                model.addHeader("Test2");
                
                model.addComponent("Test0", "TestComponent0");
                model.addComponent("Test0", "TestComponent1");
                
                model.addComponent("Test1", "TestComponent2");
                model.addComponent("Test1", "TestComponent3");
            }
        });
        
        // check if things are added as expected
        JPanelFixture titlePane = fixture.panel("Test0");
        titlePane.requireVisible();
        
        JLabelFixture iconFixture = titlePane.label("Test0_ExpanderIcon");
        iconFixture.requireVisible();
        assertNotNull(iconFixture.target.getIcon());
        assertFalse(iconFixture.target.getIcon() instanceof EmptyIcon);

        // open it up
        titlePane.doubleClick();
        
        JLabelFixture component = titlePane.label("TestComponent0");
        component.requireVisible();
        component = titlePane.label("TestComponent1");
        component.requireVisible();
        
        titlePane = fixture.panel("Test1");
        titlePane.requireVisible();
        titlePane.doubleClick();
        
        iconFixture = titlePane.label("Test1_ExpanderIcon");
        iconFixture.requireVisible();
        assertNotNull(iconFixture.target.getIcon());
        assertFalse(iconFixture.target.getIcon() instanceof EmptyIcon);

        component = titlePane.label("TestComponent2");
        component.requireVisible();
        
        component = titlePane.label("TestComponent3");
        component.requireVisible();
        
        titlePane = fixture.panel("Test2");
        titlePane.requireVisible();
        titlePane.doubleClick();
        
        iconFixture = titlePane.label("Test2_ExpanderIcon");
        iconFixture.requireVisible();
        assertNotNull(iconFixture.target.getIcon());
        assertTrue(iconFixture.target.getIcon() instanceof EmptyIcon);
    }
}

