/*
 * Copyright 2012-2015 Red Hat, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JFrame;

import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;

import org.fest.swing.annotation.GUITest;
import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiTask;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JButtonFixture;
import org.fest.swing.fixture.JTextComponentFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.redhat.thermostat.annotations.internal.CacioTest;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.shared.locale.LocalizedString;

@Category(CacioTest.class)
@RunWith(CacioFESTRunner.class)
public class SearchFieldTest {

    private final String OTHER_COMPONENT_NAME = "other";

    private JFrame frame;
    private SearchField searchField;
    private JButton otherComponent;
    private FrameFixture frameFixture;

    @BeforeClass
    public static void setUpOnce() {
        FailOnThreadViolationRepaintManager.install();
    }

    @Before
    public void setUp() {
        GuiActionRunner.execute(new GuiTask() {


            @Override
            protected void executeInEDT() throws Throwable {
                frame = new JFrame();
                frame.setLayout(new FlowLayout());
                searchField = new SearchField();
                frame.add(searchField);
                otherComponent = new JButton();
                otherComponent.setName(OTHER_COMPONENT_NAME);
                frame.add(otherComponent);
            }
        });
        frameFixture = new FrameFixture(frame);
    }

    @After
    public void tearDown() {
        frameFixture.cleanUp();
        frameFixture = null;
    }

    @Test
    public void verifyInitialSearchString() {
        frameFixture.show();

        assertEquals("", searchField.getSearchText());
    }

    @Category(GUITest.class)
    @GUITest
    @Test
    public void verifyLabelShownByDefault() {
        final LocalizedString LABEL = new LocalizedString("search label to help users");
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                searchField.setLabel(LABEL);            }
        });

        frameFixture.show();
        JTextComponentFixture textBox = frameFixture.textBox(SearchField.VIEW_NAME);
        assertEquals(LABEL.getContents(), textBox.text());
    }

    @Category(GUITest.class)
    @GUITest
    @Test
    public void verifyLabelHiddenAndShownProperly() {
        final LocalizedString LABEL = new LocalizedString("search label to help users");
        final String USER_TEXT = "java";

        GuiActionRunner.execute(new GuiTask() {

            @Override
            protected void executeInEDT() throws Throwable {
                searchField.setLabel(LABEL);
            }
        });

        frameFixture.show();
        JTextComponentFixture textBox = frameFixture.textBox(SearchField.VIEW_NAME);
        assertEquals(LABEL.getContents(), textBox.text());

        textBox.enterText(USER_TEXT);

        textBox.deleteText();

        JButtonFixture button = frameFixture.button(OTHER_COMPONENT_NAME);
        button.focus();

        assertEquals(LABEL.getContents(), textBox.text());
    }

    @Category(GUITest.class)
    @Test
    public void verifySearchTextTypedIsReturned() {
        frameFixture.show();

        final String SEARCH_TEXT = "test";
        JTextComponentFixture textBox = frameFixture.textBox(SearchField.VIEW_NAME);
        textBox.enterText(SEARCH_TEXT);
        String actual = searchField.getSearchText();
        assertEquals(SEARCH_TEXT, actual);
    }

    @Category(GUITest.class)
    @GUITest
    @Test
    public void verifyTextSetIsShown() {
        frameFixture.show();

        final String SEARCH_TEXT = "test";
        GuiActionRunner.execute(new GuiTask() {
            @Override
            protected void executeInEDT() throws Throwable {
                searchField.setSearchText(SEARCH_TEXT);
            }
        });
        JTextComponentFixture textBox = frameFixture.textBox(SearchField.VIEW_NAME);
        String actual = textBox.text();
        assertEquals(SEARCH_TEXT, actual);
    }

    @Category(GUITest.class)
    @GUITest
    @Test
    public void verifyListenersAreFiredOnTextEntry() {
        frameFixture.show();

        final String SEARCH_TEXT = "test";
        ActionListener<SearchField.SearchAction> listener = mock(ActionListener.class);

        JTextComponentFixture textBox = frameFixture.textBox(SearchField.VIEW_NAME);

        searchField.addSearchListener(listener);

        textBox.enterText(SEARCH_TEXT);

        verify(listener, times(SEARCH_TEXT.length())).actionPerformed(
                new ActionEvent<SearchField.SearchAction>(searchField, SearchField.SearchAction.PERFORM_SEARCH));

        textBox.enterText("\n");

        verify(listener, times(SEARCH_TEXT.length() + 1)).actionPerformed(
                new ActionEvent<SearchField.SearchAction>(searchField, SearchField.SearchAction.PERFORM_SEARCH));

    }

}

