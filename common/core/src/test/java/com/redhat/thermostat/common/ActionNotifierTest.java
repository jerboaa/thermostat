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

package com.redhat.thermostat.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

public class ActionNotifierTest {

    private enum TestId {
        TEST_ID1
    }


    @Test
    public void verifySingleListenerNotification() {

        Object source = new Object();
        ActionNotifier<TestId> viewActionSupport = new ActionNotifier<TestId>(source);
        @SuppressWarnings("unchecked")
        ActionListener<TestId> listener = mock(ActionListener.class);
        viewActionSupport.addActionListener(listener);

        viewActionSupport.fireAction(TestId.TEST_ID1);

        verify(listener).actionPerformed(new ActionEvent<TestId>(source, TestId.TEST_ID1));
    }

    @Test
    public void verifyMultiListenerNotification() {

        Object source = new Object();
        ActionNotifier<TestId> viewActionSupport = new ActionNotifier<TestId>(source);
        @SuppressWarnings("unchecked")
        ActionListener<TestId> listener1 = mock(ActionListener.class);
        @SuppressWarnings("unchecked")
        ActionListener<TestId> listener2 = mock(ActionListener.class);
        @SuppressWarnings("unchecked")
        ActionListener<TestId> listener3 = mock(ActionListener.class);
        viewActionSupport.addActionListener(listener1);
        viewActionSupport.addActionListener(listener2);
        viewActionSupport.addActionListener(listener3);

        viewActionSupport.fireAction(TestId.TEST_ID1);

        verify(listener1).actionPerformed(new ActionEvent<TestId>(source, TestId.TEST_ID1));
        verify(listener2).actionPerformed(new ActionEvent<TestId>(source, TestId.TEST_ID1));
        verify(listener3).actionPerformed(new ActionEvent<TestId>(source, TestId.TEST_ID1));
    }

    @Test
    public void verifyListenersInvokedInPresenseOfExceptions() {
        Object source = new Object();
        ActionNotifier<TestId> notifier = new ActionNotifier<TestId>(source);
        @SuppressWarnings("unchecked")
        ActionListener<TestId> listener1 = mock(ActionListener.class);
        @SuppressWarnings("unchecked")
        ActionListener<TestId> listener2 = mock(ActionListener.class);
        doThrow(new IllegalArgumentException("ignore")).when(listener2).actionPerformed(isA(ActionEvent.class));
        @SuppressWarnings("unchecked")
        ActionListener<TestId> listener3 = mock(ActionListener.class);
        notifier.addActionListener(listener1);
        notifier.addActionListener(listener2);
        notifier.addActionListener(listener3);

        notifier.fireAction(TestId.TEST_ID1);

        verify(listener1).actionPerformed(new ActionEvent<TestId>(source, TestId.TEST_ID1));
        verify(listener2).actionPerformed(new ActionEvent<TestId>(source, TestId.TEST_ID1));
        verify(listener3).actionPerformed(new ActionEvent<TestId>(source, TestId.TEST_ID1));
    }

    @Test
    public void verifyRemoveListener() {
        Object source = new Object();
        ActionNotifier<TestId> viewActionSupport = new ActionNotifier<TestId>(source);
        @SuppressWarnings("unchecked")
        ActionListener<TestId> listener = mock(ActionListener.class);
        viewActionSupport.addActionListener(listener);
        viewActionSupport.removeActionListener(listener);

        viewActionSupport.fireAction(TestId.TEST_ID1);

        verify(listener, never()).actionPerformed(new ActionEvent<TestId>(source, TestId.TEST_ID1));
    }

    @Test
    public void testRemoveWhileNotifying() {
        Object source = new Object();
        final ActionNotifier<TestId> viewActionSupport = new ActionNotifier<TestId>(source);
        final boolean[] listenerCalled = new boolean[] { false };

        final ActionListener<TestId> listener = new ActionListener<TestId>() {

            @Override
            public void actionPerformed(ActionEvent<TestId> viewActionEvent) {
                viewActionSupport.removeActionListener(this);
                listenerCalled[0] = true;
            }
            
        };
        viewActionSupport.addActionListener(listener);

        viewActionSupport.fireAction(TestId.TEST_ID1);

        assertTrue(listenerCalled[0]);

        listenerCalled[0] = false;
        viewActionSupport.fireAction(TestId.TEST_ID1);
        assertFalse(listenerCalled[0]);
    }

}

