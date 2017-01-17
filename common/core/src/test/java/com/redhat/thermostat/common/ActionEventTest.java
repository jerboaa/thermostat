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

package com.redhat.thermostat.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ActionEventTest {

    private enum TestId {
        TEST_ID1, TEST_ID2
    }

    @Test
    public void testGetActionId() {
        ActionEvent<TestId> viewActionEvent = new ActionEvent<>(new Object(), TestId.TEST_ID1);
        assertEquals(TestId.TEST_ID1, viewActionEvent.getActionId());
    }

    @Test(expected=NullPointerException.class)
    public void verifyThatNullActionIdThrowsNPE() {
        new ActionEvent<>(new Object(), null);
    }

    @Test
    public void testEqualsEquals() {
        Object source = new Object();
        ActionEvent<TestId> action1 = new ActionEvent<>(source, TestId.TEST_ID1);
        ActionEvent<TestId> action2 = new ActionEvent<>(source, TestId.TEST_ID1);
        assertTrue(action1.equals(action2));
    }

    @Test
    public void testEqualsDifferentSource() {
        Object source1 = new Object();
        Object source2 = new Object();
        ActionEvent<TestId> action1 = new ActionEvent<>(source1, TestId.TEST_ID1);
        ActionEvent<TestId> action2 = new ActionEvent<>(source2, TestId.TEST_ID1);
        assertFalse(action1.equals(action2));
    }

    @Test
    public void testEqualsDifferentId() {
        Object source = new Object();
        ActionEvent<TestId> action1 = new ActionEvent<>(source, TestId.TEST_ID1);
        ActionEvent<TestId> action2 = new ActionEvent<>(source, TestId.TEST_ID2);
        assertFalse(action1.equals(action2));
    }

    @Test
    public void testEqualsNull() {
        Object source = new Object();
        ActionEvent<TestId> action1 = new ActionEvent<>(source, TestId.TEST_ID1);
        assertFalse(action1.equals(null));
    }

    @Test
    public void testEqualsSame() {
        Object source = new Object();
        ActionEvent<TestId> action1 = new ActionEvent<>(source, TestId.TEST_ID1);
        assertTrue(action1.equals(action1));
    }

    @Test
    public void testEqualsOtherType() {
        Object source = new Object();
        ActionEvent<TestId> action1 = new ActionEvent<>(source, TestId.TEST_ID1);
        assertFalse(action1.equals("test"));
    }

    @Test
    public void testHashCodeEquals() {
        Object source = new Object();
        ActionEvent<TestId> action1 = new ActionEvent<>(source, TestId.TEST_ID1);
        ActionEvent<TestId> action2 = new ActionEvent<>(source, TestId.TEST_ID1);
        assertTrue(action1.hashCode() == action2.hashCode());
    }

    @Test
    public void testHashCodeStaysSame() {
        Object source = new Object();
        ActionEvent<TestId> action1 = new ActionEvent<>(source, TestId.TEST_ID1);
        int hashCode1 = action1.hashCode();
        int hashCode2 = action1.hashCode();
        assertTrue(hashCode1 == hashCode2);
    }
}

