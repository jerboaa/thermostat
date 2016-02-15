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

package com.redhat.thermostat.client.swing.components;

import org.fest.swing.annotation.GUITest;
import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiTask;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

public class ThermostatTextFieldTest {

    private ThermostatTextField textField;

    @BeforeClass
    public static void setupOnce() {
        FailOnThreadViolationRepaintManager.install();
    }

    @Before
    public void setUp() {
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                textField = new ThermostatTextField();
            }
        });
    }

    @Category(GUITest.class)
    @GUITest
    @Test
    public void testComponentPopupMenuIsSet() {
        assertThat(textField.getContextMenu(), is(not(equalTo(null))));
        assertThat(textField.getContextMenu(), is(textField.getComponentPopupMenu()));
    }

    @Category(GUITest.class)
    @GUITest
    @Test
    public void testMenuEnabledWhenParentEnabled() {
        assertThat(textField.getContextMenu().isEnabled(), is(true));

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                textField.setEnabled(false);
            }
        });

        assertThat(textField.getContextMenu().isEnabled(), is(false));

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                textField.setEnabled(true);
            }
        });

        assertThat(textField.getContextMenu().isEnabled(), is(true));
    }

    @Category(GUITest.class)
    @GUITest
    @Test
    public void testCutDisabledWhenParentDisabled() {
        assertThat(textField.getContextMenu().isCutEnabled(), is(true));

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                textField.setEnabled(false);
            }
        });

        assertThat(textField.getContextMenu().isCutEnabled(), is(false));

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                textField.setEnabled(true);
            }
        });

        assertThat(textField.getContextMenu().isCutEnabled(), is(true));
    }

    @Category(GUITest.class)
    @GUITest
    @Test
    public void testCopyDisabledWhenParentDisabled() {
        assertThat(textField.getContextMenu().isCopyEnabled(), is(true));

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                textField.setEnabled(false);
            }
        });

        assertThat(textField.getContextMenu().isCopyEnabled(), is(false));

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                textField.setEnabled(true);
            }
        });

        assertThat(textField.getContextMenu().isCopyEnabled(), is(true));
    }

    @Category(GUITest.class)
    @GUITest
    @Test
    public void testPasteDisabledWhenParentDisabled() {
        assertThat(textField.getContextMenu().isPasteEnabled(), is(true));

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                textField.setEnabled(false);
            }
        });

        assertThat(textField.getContextMenu().isPasteEnabled(), is(false));

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                textField.setEnabled(true);
            }
        });

        assertThat(textField.getContextMenu().isPasteEnabled(), is(true));
    }

    @Category(GUITest.class)
    @GUITest
    @Test
    public void testCutDisabledWhenParentNotEditable() {
        assertThat(textField.getContextMenu().isCutEnabled(), is(true));

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                textField.setEditable(false);
            }
        });

        assertThat(textField.getContextMenu().isCutEnabled(), is(false));

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                textField.setEditable(true);
            }
        });

        assertThat(textField.getContextMenu().isCutEnabled(), is(true));
    }

    @Category(GUITest.class)
    @GUITest
    @Test
    public void testPasteDisabledWhenParentNotEditable() {
        assertThat(textField.getContextMenu().isPasteEnabled(), is(true));

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                textField.setEditable(false);
            }
        });

        assertThat(textField.getContextMenu().isPasteEnabled(), is(false));

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                textField.setEditable(true);
            }
        });

        assertThat(textField.getContextMenu().isPasteEnabled(), is(true));
    }

    @Category(GUITest.class)
    @GUITest
    @Test
    public void testCopyNotDisabledWhenParentNotEditable() {
        assertThat(textField.getContextMenu().isCopyEnabled(), is(true));

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                textField.setEditable(false);
            }
        });

        assertThat(textField.getContextMenu().isCopyEnabled(), is(true));

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                textField.setEditable(true);
            }
        });

        assertThat(textField.getContextMenu().isCopyEnabled(), is(true));
    }

    @Category(GUITest.class)
    @GUITest
    @Test
    public void testEnableDoesNotOverrideEditable() {
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                textField.setEnabled(false);
                textField.setEditable(false);
            }
        });

        assertThat(textField.getContextMenu().isCutEnabled(), is(false));
        assertThat(textField.getContextMenu().isCopyEnabled(), is(false));
        assertThat(textField.getContextMenu().isPasteEnabled(), is(false));

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                textField.setEnabled(true);
            }
        });

        assertThat(textField.getContextMenu().isCutEnabled(), is(false));
        assertThat(textField.getContextMenu().isCopyEnabled(), is(true));
        assertThat(textField.getContextMenu().isPasteEnabled(), is(false));
    }

    @Category(GUITest.class)
    @GUITest
    @Test
    public void testEditableDoesNotOverrideEnabled() {
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                textField.setEnabled(false);
                textField.setEditable(false);
            }
        });

        assertThat(textField.getContextMenu().isCutEnabled(), is(false));
        assertThat(textField.getContextMenu().isCopyEnabled(), is(false));
        assertThat(textField.getContextMenu().isPasteEnabled(), is(false));

        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                textField.setEditable(true);
            }
        });

        assertThat(textField.getContextMenu().isCutEnabled(), is(false));
        assertThat(textField.getContextMenu().isCopyEnabled(), is(false));
        assertThat(textField.getContextMenu().isPasteEnabled(), is(false));
    }

}
