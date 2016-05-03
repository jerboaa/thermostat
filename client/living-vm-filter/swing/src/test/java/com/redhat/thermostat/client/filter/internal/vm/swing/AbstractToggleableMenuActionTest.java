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

package com.redhat.thermostat.client.filter.internal.vm.swing;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.matchers.GreaterOrEqual;
import org.mockito.internal.matchers.LessOrEqual;

import com.redhat.thermostat.client.ui.MenuAction;
import com.redhat.thermostat.client.ui.ToggleableReferenceFieldLabelDecorator;
import com.redhat.thermostat.shared.locale.LocalizedString;

public abstract class AbstractToggleableMenuActionTest<T extends MenuAction> {

    protected ToggleableReferenceFieldLabelDecorator decorator;
    protected T action;

    @Before
    public abstract void setup();

    @Test
    public void verifyExecuteTogglesDecorator() {
        when(decorator.isEnabled()).thenReturn(false);
        action.execute();
        verify(decorator).setEnabled(true);
        when(decorator.isEnabled()).thenReturn(true);
        action.execute();
        verify(decorator).setEnabled(false);
    }

    @Test
    public void assertSortOrderWithinBounds() {
        int sortOrder = action.sortOrder();
        assertThat(sortOrder, is(new GreaterOrEqual<>(MenuAction.SORT_TOP)));
        assertThat(sortOrder, is(new LessOrEqual<>(MenuAction.SORT_BOTTOM)));
    }

    @Test
    public void assertPersistenceIdContainsMenuKey() {
        assertThat(action.getPersistenceID(), containsString(MenuAction.MENU_KEY));
    }

    @Test
    public void assertPathContainsName() {
        assertThat(action.getPath(), containsLocalizedString(action.getName()));
    }

    private static Matcher<LocalizedString[]> containsLocalizedString(final LocalizedString ls) {
        return new BaseMatcher<LocalizedString[]>() {
            @Override
            public boolean matches(Object o) {
                if (!(o instanceof LocalizedString[])) {
                    return false;
                }
                boolean contains = false;
                for (LocalizedString str : ((LocalizedString[]) o)) {
                    contains = contains || str.getContents().equals(ls.getContents());
                }
                return contains;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("LocalizedString[] containing element with contents ")
                        .appendValue(ls.getContents());
            }
        };
    }

}
