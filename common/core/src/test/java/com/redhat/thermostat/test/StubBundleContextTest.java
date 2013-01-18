/*
 * Copyright 2013 Red Hat, Inc.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Thermostat; see the file COPYING. If not see
 * <http://www.gnu.org/licenses/>.
 *
 * Linking this code with other modules is making a combined work
 * based on this code. Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this code give
 * you permission to link this code with independent modules to
 * produce an executable, regardless of the license terms of these
 * independent modules, and to copy and distribute the resulting
 * executable under terms of your choice, provided that you also
 * meet, for each linked independent module, the terms and conditions
 * of the license of that module. An independent module is a module
 * which is not derived from or based on this code. If you modify
 * this code, you may extend this exception to your version of the
 * library, but you are not obligated to do so. If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.redhat.thermostat.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Hashtable;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.test.StubBundleContext.ServiceInformation;

/**
 * Test that StubBundleContext behaves like a BundleContext, based on what is
 * specified in the OSGi spec. Other optional methods to help in testing are
 * great, but adherence to spec is most important.
 */
public class StubBundleContextTest {

    private StubBundleContext bundleContext;

    @Before
    public void setUp() {
        bundleContext = new StubBundleContext();
        assertNotNull(bundleContext);
    }

    @Test
    public void testSetAndGetContextBundle() {
        assertEquals(null, bundleContext.getBundle());

        Bundle bundle = mock(Bundle.class);
        bundleContext.setBundle(bundle);
        assertSame(bundle, bundleContext.getBundle());
    }

    @Test(expected = NotImplementedException.class)
    public void testInstallBundleFromLocation() throws BundleException {
        bundleContext.installBundle("");
    }

    @Test(expected = NotImplementedException.class)
    public void testInstallBundleFromInputStream() throws BundleException {
        bundleContext.installBundle("", new ByteArrayInputStream(new byte[0]));
    }

    @Test
    public void testSetAndGetBundles() {
        assertEquals(null, bundleContext.getBundle(0));

        Bundle systemBundle = mock(Bundle.class);
        bundleContext.setBundle(0, systemBundle);

        assertEquals(systemBundle, bundleContext.getBundle(0));

        assertArrayEquals(new Bundle[] { systemBundle }, bundleContext.getBundles());
    }

    @Test(expected = NotImplementedException.class)
    public void testGetBundleByLargeId() {
        bundleContext.getBundle(Long.MAX_VALUE);
    }

    @Test(expected = NotImplementedException.class)
    public void testGetBundleByLocation() {
        bundleContext.getBundle("");
    }

    @Test
    public void testAddRemoveFrameworkListener() {
        try {
            bundleContext.addFrameworkListener(mock(FrameworkListener.class));
            fail("not expected");
        } catch (NotImplementedException notImplemented) {
            /* okay: expected */
        }

        try {
            bundleContext.removeFrameworkListener(mock(FrameworkListener.class));
            fail("not expected");
        } catch (NotImplementedException notImplemented) {
            /* okay: expected */
        }
    }

    @Test
    public void testAddRemoveBundleListener() {
        try {
            bundleContext.addBundleListener(mock(BundleListener.class));
            fail("not expected");
        } catch (NotImplementedException notImplemented) {
            /* okay: expected */
        }

        try {
            bundleContext.removeBundleListener(mock(BundleListener.class));
            fail("not expected");
        } catch (NotImplementedException notImplemented) {
            /* okay: expected */
        }
    }

    @Test
    public void testGetPropertyDelegatesToSystemProperties() {
        assertEquals(System.getProperty("user.name"), bundleContext.getProperty("user.name"));
    }

    @Test
    public void testGetAndSetProperty() {
        final String PROPERTY_NAME = "foo.bar.baz";
        final String PROPERTY_NEW_VALUE = "spam.eggs";

        assertEquals(null, bundleContext.getProperty(PROPERTY_NAME));

        bundleContext.setProperty(PROPERTY_NAME, PROPERTY_NEW_VALUE);

        assertEquals(PROPERTY_NEW_VALUE, bundleContext.getProperty(PROPERTY_NAME));
    }

    @Test
    public void testFilterCreationReturnsASaneFilter() throws InvalidSyntaxException {
        Filter filter = bundleContext.createFilter("(foo=bar)");
        assertNotNull(filter);

        Hashtable<String, String> dict = new Hashtable<>();
        dict.put("foo", "bar");

        assertTrue(filter.match(dict));
    }

    @Test
    public void testAddRemoveService() {
        TestService service = mock(TestService.class);

        assertEquals(0, bundleContext.getAllServices().size());
        assertFalse(bundleContext.isServiceRegistered(TestService.class.getName(), service.getClass()));

        ServiceRegistration registration = bundleContext.registerService(TestService.class, service, null);

        assertEquals(1, bundleContext.getAllServices().size());
        assertTrue(bundleContext.isServiceRegistered(TestService.class.getName(), service.getClass()));

        registration.unregister();

        assertEquals(0, bundleContext.getAllServices().size());
        assertFalse(bundleContext.isServiceRegistered(TestService.class.getName(), service.getClass()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddServiceThatDoesNotImplementInterfaceClass() {
        AnotherTestService service = mock(AnotherTestService.class);

        bundleContext.registerService(TestService.class.getName(), service, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddServiceThatDoesNotImplementInterfaceClass2() {
        AnotherTestService service = mock(AnotherTestService.class);

        bundleContext.registerService("foo.bar.Baz", service, null);
    }

    @Test
    public void testAddServiceWithProperties() {
        TestService service = mock(TestService.class);

        Hashtable props = new Hashtable();
        props.put(Constants.SERVICE_ID, -10);
        props.put(Constants.SERVICE_RANKING, 10);
        props.put(Constants.OBJECTCLASS, AnotherTestService.class.getName());

        ServiceRegistration reg = bundleContext.registerService(TestService.class, service, props);

        ServiceReference serviceRef = reg.getReference();

        assertArrayEquals(new String[] { TestService.class.getName() },
                (String[]) serviceRef.getProperty(Constants.OBJECTCLASS));

        assertEquals(10, serviceRef.getProperty(Constants.SERVICE_RANKING));
        assertEquals(0, serviceRef.getProperty(Constants.SERVICE_ID));
    }

    @Test
    public void testAddServiceWithNonIntegerRanking() {
        TestService service = mock(TestService.class);

        Hashtable props = new Hashtable();
        props.put(Constants.SERVICE_RANKING, new Object());

        ServiceRegistration reg = bundleContext.registerService(TestService.class, service, props);

        ServiceReference serviceRef = reg.getReference();

        assertEquals(0, serviceRef.getProperty(Constants.SERVICE_RANKING));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveUnknownService() {
        bundleContext.removeService(new ServiceInformation(mock(TestService.class), new Hashtable()));
    }

    @Test
    public void testAddingAndRemovingListenerWorks() {
        assertEquals(0, bundleContext.getServiceListeners().size());

        ServiceListener listener = mock(ServiceListener.class);

        bundleContext.addServiceListener(listener);

        assertEquals(1, bundleContext.getServiceListeners().size());
        assertEquals(listener, bundleContext.getServiceListeners().toArray()[0]);

        bundleContext.removeServiceListener(listener);

        assertEquals(0, bundleContext.getServiceListeners().size());
    }

    @Test
    public void testAddingSameListenerReplacedTheOldOne() {
        assertEquals(0, bundleContext.getServiceListeners().size());

        ServiceListener listener = mock(ServiceListener.class);

        bundleContext.addServiceListener(listener);

        assertEquals(1, bundleContext.getServiceListeners().size());

        bundleContext.addServiceListener(listener);

        assertEquals(1, bundleContext.getServiceListeners().size());
    }

    @Test
    public void testRemovingAnUnknownListenerIsANoOp() {
        ServiceListener listener = mock(ServiceListener.class);
        bundleContext.addServiceListener(listener);
        assertEquals(1, bundleContext.getServiceListeners().size());

        ServiceListener unknownListener = mock(ServiceListener.class);
        bundleContext.removeServiceListener(unknownListener);

        assertEquals(1, bundleContext.getServiceListeners().size());
    }

    @Test
    public void testServiceListenersWithoutFiltersWork() {
        ServiceListener listener = mock(ServiceListener.class);
        bundleContext.addServiceListener(listener);

        bundleContext.registerService(TestService.class, mock(TestService.class), null);

        assertListenerRecievedRegisteredEvent(listener);
    }

    @Test
    public void testServiceListenersWithFiltersWork() throws InvalidSyntaxException {
        ServiceListener specificListener = mock(ServiceListener.class);
        String specificFilter = "(" + Constants.OBJECTCLASS + "=" + TestService.class.getName() + ")";
        bundleContext.addServiceListener(specificListener, specificFilter);

        ServiceListener allListener = mock(ServiceListener.class);
        String allListenerFilter = "(" + Constants.OBJECTCLASS + "=*)";
        bundleContext.addServiceListener(allListener, allListenerFilter);

        ServiceListener otherListener = mock(ServiceListener.class);
        String otherListenerFilter = "(" + Constants.OBJECTCLASS + "=foo.bar.Baz)";
        bundleContext.addServiceListener(otherListener, otherListenerFilter);

        ServiceRegistration registration = bundleContext.registerService(TestService.class, mock(TestService.class), null);

        assertListenerRecievedRegisteredEvent(specificListener);
        assertListenerRecievedRegisteredEvent(allListener);

        registration.unregister();

        assertListenerRecievedUnregisteringEvent(specificListener);
        assertListenerRecievedUnregisteringEvent(allListener);

        verify(otherListener, never()).serviceChanged(isA(ServiceEvent.class));

    }

    private void assertListenerRecievedRegisteredEvent(ServiceListener listener) {
        ArgumentCaptor<ServiceEvent> eventCaptor = ArgumentCaptor.forClass(ServiceEvent.class);
        verify(listener).serviceChanged(eventCaptor.capture());
        assertEquals(1, eventCaptor.getAllValues().size());
        ServiceEvent event = eventCaptor.getValue();
        assertEquals(ServiceEvent.REGISTERED, event.getType());
        ServiceReference ref = event.getServiceReference();
        assertTrue(ref instanceof StubServiceReference);
    }

    private void assertListenerRecievedUnregisteringEvent(ServiceListener listener) {
        ArgumentCaptor<ServiceEvent> eventCaptor = ArgumentCaptor.forClass(ServiceEvent.class);
        verify(listener, atLeast(1)).serviceChanged(eventCaptor.capture());

        ServiceEvent event = eventCaptor.getValue();
        assertEquals(ServiceEvent.UNREGISTERING, event.getType());
        ServiceReference ref = event.getServiceReference();
        assertTrue(ref instanceof StubServiceReference);
    }

    @Test
    public void testGetServiceReferenceReturnsNullForNoMatch() {
        assertNull(bundleContext.getServiceReference(TestService.class));
    }

    @Test
    public void testGetServiceReferenceReturnsServiceWithHighestServiceRanking() {
        TestService service1 = mock(TestService.class);
        TestService service2 = mock(TestService.class);

        Hashtable service2Props = new Hashtable();
        service2Props.put(Constants.SERVICE_RANKING, 1000);

        bundleContext.registerService(TestService.class, service1, null);
        bundleContext.registerService(TestService.class, service2, service2Props);

        ServiceReference ref = bundleContext.getServiceReference(TestService.class);

        assertSame(service2, bundleContext.getService(ref));
    }

    @Test
    public void testGetServiceReferenceReturnsServiceWithLowerServiceId() {
        TestService service1 = mock(TestService.class);
        TestService service2 = mock(TestService.class);

        bundleContext.registerService(TestService.class, service1, null);
        bundleContext.registerService(TestService.class, service2, null);

        ServiceReference ref = bundleContext.getServiceReference(TestService.class);

        assertSame(service1, bundleContext.getService(ref));
    }

    @Test
    public void testGetServiceReferences() throws InvalidSyntaxException {
        TestService service = mock(TestService.class);

        bundleContext.registerService(TestService.class, service, null);
        bundleContext.registerService(AnotherTestService.class, mock(AnotherTestService.class), null);

        Collection refs = bundleContext.getServiceReferences(TestService.class, null);

        assertEquals(1, refs.size());
        assertEquals(service, ((StubServiceReference) refs.toArray()[0]).getInformation().implementation);
    }

    @Test
    public void testGetAllServiceReferencesNoMatch() throws InvalidSyntaxException {
        TestService service = mock(TestService.class);

        bundleContext.registerService(TestService.class, service, null);
        bundleContext.registerService(TestService.class, mock(TestService.class), null);

        ServiceReference[] refs = bundleContext.getAllServiceReferences(AnotherTestService.class.getName(), null);

        assertEquals(null, (Object) refs);
    }

    @Test
    public void testGetAllServiceReferencesWithoutFilter() throws InvalidSyntaxException {
        TestService service = mock(TestService.class);

        bundleContext.registerService(TestService.class, service, null);
        bundleContext.registerService(AnotherTestService.class, mock(AnotherTestService.class), null);

        ServiceReference[] refs = bundleContext.getAllServiceReferences(TestService.class.getName(), null);

        assertEquals(1, refs.length);
        assertEquals(service, ((StubServiceReference) refs[0]).getInformation().implementation);
    }

    @Test
    public void testGetAllServiceReferencesWithFilter() throws InvalidSyntaxException {
        TestService service = mock(TestService.class);

        bundleContext.registerService(TestService.class, service, null);
        bundleContext.registerService(AnotherTestService.class, mock(AnotherTestService.class), null);

        String filter = "(" + Constants.OBJECTCLASS + "=" + TestService.class.getName() + ")";
        ServiceReference[] refs = bundleContext.getAllServiceReferences(TestService.class.getName(), filter);

        assertEquals(1, refs.length);
        assertEquals(service, ((StubServiceReference) refs[0]).getInformation().implementation);
    }

    @Test
    public void testGetAllServiceReferencesWithNoClassNameJustFilter() throws InvalidSyntaxException {
        TestService service = mock(TestService.class);

        bundleContext.registerService(TestService.class, service, null);
        bundleContext.registerService(AnotherTestService.class, mock(AnotherTestService.class), null);

        String filter = "(" + Constants.OBJECTCLASS + "=" + TestService.class.getName() + ")";
        ServiceReference[] refs = bundleContext.getAllServiceReferences(null, filter);

        assertEquals(1, refs.length);
        assertEquals(service, ((StubServiceReference) refs[0]).getInformation().implementation);
    }

    @Test
    public void testGetServiceUsingClassObject() {
        TestService service = mock(TestService.class);

        bundleContext.registerService(TestService.class, service, null);

        ServiceReference ref = bundleContext.getServiceReference(TestService.class);
        assertNotNull(ref);

        Object returnedService = bundleContext.getService(ref);

        assertSame(service, returnedService);
    }

    @Test
    public void testGetServiceUsingClassName() {
        TestService service = mock(TestService.class);

        bundleContext.registerService(TestService.class, service, null);

        ServiceReference ref = bundleContext.getServiceReference(TestService.class.getName());
        assertNotNull(ref);

        Object returnedService = bundleContext.getService(ref);

        assertSame(service, returnedService);
    }

    @Test
    public void testUngetService() {
        bundleContext.registerService(TestService.class, mock(TestService.class), null);

        ServiceReference ref = bundleContext.getServiceReference(TestService.class.getName());
        Object returnedService = bundleContext.getService(ref);

        assertTrue(bundleContext.ungetService(ref));

        assertFalse(bundleContext.ungetService(ref));
    }

    @Test
    public void testUngetServiceOnUnregisteredService() {
        ServiceRegistration registration = bundleContext.registerService(TestService.class, mock(TestService.class), null);

        ServiceReference ref = bundleContext.getServiceReference(TestService.class.getName());
        Object returnedService = bundleContext.getService(ref);

        registration.unregister();

        assertFalse(bundleContext.ungetService(ref));
    }

    @Test
    public void testReferenceExportCountIsCorrect() {
        TestService service = mock(TestService.class);

        ServiceRegistration reg = bundleContext.registerService(TestService.class, service, null);

        ServiceReference ref = bundleContext.getServiceReference(TestService.class);
        assertNotNull(ref);

        assertEquals(0, bundleContext.getExportedServiceCount(reg));

        Object returnedService = bundleContext.getService(ref);

        assertEquals(1, bundleContext.getExportedServiceCount(reg));

        bundleContext.ungetService(ref);

        assertEquals(0, bundleContext.getExportedServiceCount(reg));
    }

    @Test(expected = NotImplementedException.class)
    public void testGetDataFile() {
        bundleContext.getDataFile("");
    }

    @Test
    public void testIsServiceRegisteredForRegisteredService() {
        ComplexService service = mock(ComplexService.class);

        String[] ifaces = new String[] { TestService.class.getName(), AnotherTestService.class.getName() };
        ServiceRegistration reg = bundleContext.registerService(ifaces, service, null);

        assertTrue(bundleContext.isServiceRegistered(AnotherTestService.class.getName(), service.getClass()));

    }

    @Test
    public void testIsServiceRegisteredForUnregisteredService() {
        ComplexService service = mock(ComplexService.class);
        TestService someOtherService = mock(TestService.class);

        String[] ifaces = new String[] { TestService.class.getName(), AnotherTestService.class.getName() };
        ServiceRegistration reg = bundleContext.registerService(ifaces, service, null);

        assertFalse(bundleContext.isServiceRegistered(AnotherTestService.class.getName(), someOtherService.getClass()));
    }

    /*
     * Dummy service interfaces
     */

    static interface TestService { /* just a marker */}

    static interface AnotherTestService { /* just a marker */}

    static interface ComplexService extends TestService, AnotherTestService { /* ditto */}
}
