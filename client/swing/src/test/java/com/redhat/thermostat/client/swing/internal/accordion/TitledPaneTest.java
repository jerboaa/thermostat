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

import static org.junit.Assert.assertSame;

import java.awt.Color;
import java.awt.Graphics2D;

import javax.swing.JFrame;
import javax.swing.JPanel;

import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;

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
import com.redhat.thermostat.client.swing.components.VerticalLayout;

@Category(CacioTest.class)
@RunWith(CacioFESTRunner.class)
public class TitledPaneTest {

    private FrameFixture fixture;

    private static class TitleBGPainter1 implements TitledPanePainter {
                
        @Override
        public void paint(Graphics2D g, AccordionComponent pane, int width, int height) {
            if (pane.isSelected()) {
                g.setColor(Color.BLACK);
            } else {
                g.setColor(Color.BLUE);
            }
            g.fillRect(0, 0, width, height);
        }
        
        @Override
        public Color getSelectedForeground() {
            return Color.BLACK;
        }
        
        @Override
        public Color getUnselectedForeground() {
            return Color.RED;
        }
    }
    
    private static class TitleBGPainter2 implements TitledPanePainter {
        
        @Override
        public void paint(Graphics2D g, AccordionComponent pane, int width, int height) {
            if (pane.isSelected()) {
                g.setColor(Color.BLACK);
            } else {
                g.setColor(Color.BLUE);
            }
            g.fillRect(0, 0, width, height);
        }
        
        @Override
        public Color getSelectedForeground() {
            return Color.BLACK;
        }
        
        @Override
        public Color getUnselectedForeground() {
            return Color.RED;
        }
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

                JPanel container = new JPanel();
                container.setLayout(new VerticalLayout());
                
                TitledPane pane1 = new TitledPane("test1", new TitleBGPainter1());
                TitledPane pane2 = new TitledPane("test2", new TitleBGPainter2());
                
                container.add(pane1);
                container.add(pane2);
                
                frame.add(container);
                
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
    
    @Test
    public void testColorDifferences() {
        
        fixture.show();
        final JPanelFixture titlePane1 = fixture.panel("test1");
        final JPanelFixture titlePane2 = fixture.panel("test2");

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                // TitledPane do not (yet) have a model to keep track of
                // selection, this is done currently in the Accordion
                // component controller
                TitledPane pane1 = (TitledPane) titlePane1.target;                
                pane1.setSelected(true);
                
                TitledPane pane2 = (TitledPane) titlePane2.target;                
                pane2.setSelected(false);                
            }
        });
        
        JLabelFixture label1 = titlePane1.label("test1_label");
        JLabelFixture label2 = titlePane2.label("test2_label");

        assertSame(Color.BLACK, label1.foreground().target());
        assertSame(Color.RED, label2.foreground().target());

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                TitledPane pane1 = (TitledPane) titlePane1.target;                
                pane1.setSelected(false);
                
                TitledPane pane2 = (TitledPane) titlePane2.target;                
                pane2.setSelected(true);                 
            }
        });

        assertSame(Color.RED, label1.foreground().target());
        assertSame(Color.BLACK, label2.foreground().target());
    }
}

