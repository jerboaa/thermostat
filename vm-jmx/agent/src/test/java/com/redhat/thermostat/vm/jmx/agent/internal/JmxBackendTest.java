/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.vm.jmx.agent.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.agent.command.ReceiverRegistry;
import com.redhat.thermostat.agent.command.RequestReceiver;
import com.redhat.thermostat.agent.utils.management.MXBeanConnection;
import com.redhat.thermostat.agent.utils.management.MXBeanConnectionPool;
import com.redhat.thermostat.common.Ordered;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.vm.jmx.common.JmxNotification;
import com.redhat.thermostat.vm.jmx.common.JmxNotificationDAO;

public class JmxBackendTest {

    private Version version;
    private ReceiverRegistry registry;
    private MXBeanConnectionPool pool;
    private JmxNotificationDAO dao;
    private JmxBackend backend;
    private RequestReceiver receiver;

    @Before
    public void setUp() {
        version = mock(Version.class);
        when(version.getVersionNumber()).thenReturn("foo");

        registry = mock(ReceiverRegistry.class);
        receiver = mock(RequestReceiver.class);
        dao = mock(JmxNotificationDAO.class);
        pool = mock(MXBeanConnectionPool.class);

        WriterID id = mock(WriterID.class);
        backend = new JmxBackend(version, registry, dao, pool, receiver, id);
    }

    @Test
    public void testOrderValue() {
        assertTrue(backend.getOrderValue() > Ordered.ORDER_USER_GROUP);
    }

    @Test
    public void testActivation() {
        assertFalse(backend.isActive());
        assertTrue(backend.activate());
        assertTrue(backend.isActive());
        assertTrue(backend.activate());
        assertTrue(backend.isActive());
    }

    @Test
    public void testDeactivation() {
        assertTrue(backend.activate());

        assertTrue(backend.isActive());

        assertTrue(backend.deactivate());

        assertFalse(backend.isActive());
    }

    @Test
    public void testDectivateTwice() {
        assertTrue(backend.activate());
        assertTrue(backend.isActive());

        assertTrue(backend.deactivate());
        assertFalse(backend.isActive());
        assertTrue(backend.deactivate());
        assertFalse(backend.isActive());
    }

    @Test
    public void testNotificationsEnabled() throws Exception {
        ObjectName name1 = mock(ObjectName.class);
        Set<ObjectName> names = new HashSet<ObjectName>();
        names.add(name1);

        MBeanInfo info1 = mock(MBeanInfo.class);
        when(info1.getNotifications()).thenReturn(new MBeanNotificationInfo[10]);

        MBeanServerConnection actual = mock(MBeanServerConnection.class);
        when(actual.queryNames(null, null)).thenReturn(names);
        when(actual.getMBeanInfo(name1)).thenReturn(info1);

        MXBeanConnection connection = mock(MXBeanConnection.class);
        when(connection.get()).thenReturn(actual);
        when(pool.acquire(42)).thenReturn(connection);

        backend.enableNotificationsFor("42", 42);

        verify(actual).addNotificationListener(eq(name1), any(NotificationListener.class), eq((NotificationFilter) null), any());
    }

    @Test
    public void testJmxNotificationAddedToStorage() throws Exception {
        ObjectName name1 = mock(ObjectName.class);
        Set<ObjectName> names = new HashSet<ObjectName>();
        names.add(name1);

        MBeanInfo info1 = mock(MBeanInfo.class);
        when(info1.getNotifications()).thenReturn(new MBeanNotificationInfo[10]);

        MBeanServerConnection actual = mock(MBeanServerConnection.class);
        when(actual.queryNames(null, null)).thenReturn(names);
        when(actual.getMBeanInfo(name1)).thenReturn(info1);

        MXBeanConnection connection = mock(MXBeanConnection.class);
        when(connection.get()).thenReturn(actual);
        when(pool.acquire(42)).thenReturn(connection);

        backend.enableNotificationsFor("42", 42);

        ArgumentCaptor<NotificationListener> listenerCaptor = ArgumentCaptor.forClass(NotificationListener.class);
        ArgumentCaptor<Object> handbackCaptor = ArgumentCaptor.forClass(Object.class);

        verify(actual).addNotificationListener(eq(name1), listenerCaptor.capture(), eq((NotificationFilter) null), handbackCaptor.capture());

        NotificationListener listener = listenerCaptor.getValue();

        Notification notification = mock(Notification.class);
        when(notification.toString()).thenReturn("testing");
        when(notification.getSource()).thenReturn(name1);

        listener.handleNotification(notification, handbackCaptor.getValue());

        verify(dao).addNotification(isA(JmxNotification.class));
    }

    @Test
    public void testMBeanRegisteredNotification() throws Exception {
        ObjectName serverBean = MBeanServerDelegate.DELEGATE_NAME;
        Set<ObjectName> names = new HashSet<ObjectName>();
        names.add(serverBean);

        MBeanInfo serverBeanInfo = mock(MBeanInfo.class);
        when(serverBeanInfo.getNotifications()).thenReturn(new MBeanNotificationInfo[10]);

        ObjectName newBeanName = mock(ObjectName.class);
        MBeanInfo newBeanInfo = mock(MBeanInfo.class);
        when(newBeanInfo.getNotifications()).thenReturn(new MBeanNotificationInfo[1]);

        MBeanServerNotification newMBeanNotification = mock(MBeanServerNotification.class);
        when(newMBeanNotification.getType()).thenReturn(MBeanServerNotification.REGISTRATION_NOTIFICATION);
        when(newMBeanNotification.getMBeanName()).thenReturn(newBeanName);

        MBeanServerConnection actual = mock(MBeanServerConnection.class);
        when(actual.queryNames(null, null)).thenReturn(names);
        when(actual.getMBeanInfo(serverBean)).thenReturn(serverBeanInfo);
        when(actual.getMBeanInfo(newBeanName)).thenReturn(newBeanInfo);

        MXBeanConnection connection = mock(MXBeanConnection.class);
        when(connection.get()).thenReturn(actual);
        when(pool.acquire(42)).thenReturn(connection);

        backend.enableNotificationsFor("42", 42);

        ArgumentCaptor<NotificationListener> listenerCaptor = ArgumentCaptor.forClass(NotificationListener.class);
        ArgumentCaptor<Object> handbackCaptor = ArgumentCaptor.forClass(Object.class);

        verify(actual).addNotificationListener(eq(serverBean), listenerCaptor.capture(), eq((NotificationFilter) null), handbackCaptor.capture());

        NotificationListener listener = listenerCaptor.getValue();

        listener.handleNotification(newMBeanNotification, handbackCaptor.getValue());

        verify(actual).addNotificationListener(eq(newBeanName), isA(NotificationListener.class), eq((NotificationFilter) null), eq(handbackCaptor.getValue()));
    }

    @Test
    public void testMBeanUnregisteredNotification() throws Exception {
        ObjectName serverBean = MBeanServerDelegate.DELEGATE_NAME;
        Set<ObjectName> names = new HashSet<ObjectName>();
        names.add(serverBean);

        MBeanInfo serverBeanInfo = mock(MBeanInfo.class);
        when(serverBeanInfo.getNotifications()).thenReturn(new MBeanNotificationInfo[10]);

        MBeanServerNotification mBeanRemovedNotification = mock(MBeanServerNotification.class);
        when(mBeanRemovedNotification.getType()).thenReturn(MBeanServerNotification.UNREGISTRATION_NOTIFICATION);

        MBeanServerConnection actual = mock(MBeanServerConnection.class);
        when(actual.queryNames(null, null)).thenReturn(names);
        when(actual.getMBeanInfo(serverBean)).thenReturn(serverBeanInfo);

        MXBeanConnection connection = mock(MXBeanConnection.class);
        when(connection.get()).thenReturn(actual);
        when(pool.acquire(42)).thenReturn(connection);

        backend.enableNotificationsFor("42", 42);

        ArgumentCaptor<NotificationListener> listenerCaptor = ArgumentCaptor.forClass(NotificationListener.class);
        ArgumentCaptor<Object> handbackCaptor = ArgumentCaptor.forClass(Object.class);

        verify(actual).addNotificationListener(eq(serverBean), listenerCaptor.capture(), eq((NotificationFilter) null), handbackCaptor.capture());

        NotificationListener listener = listenerCaptor.getValue();

        verify(actual).queryNames(null, null);
        verify(actual).addNotificationListener(eq(MBeanServerDelegate.DELEGATE_NAME), isA(NotificationListener.class), eq((NotificationFilter) null), isA(Object.class));

        listener.handleNotification(mBeanRemovedNotification, handbackCaptor.getValue());

        verifyNoMoreInteractions(actual);
    }

    @Test
    public void testNotificationsDisabled() throws Exception {
        Set<ObjectName> names = new HashSet<ObjectName>();

        MBeanServerConnection actual = mock(MBeanServerConnection.class);
        when(actual.queryNames(null, null)).thenReturn(names);

        MXBeanConnection connection = mock(MXBeanConnection.class);
        when(connection.get()).thenReturn(actual);
        when(pool.acquire(42)).thenReturn(connection);

        backend.enableNotificationsFor("42", 42);

        backend.disableNotificationsFor("42", 42);

        verify(pool).release(42, connection);
    }
}

