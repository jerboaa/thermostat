package com.redhat.thermostat.client.ui;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.client.ui.HostVmFilter;
import com.redhat.thermostat.common.dao.VmRef;

public class HostVmFilterTest {

    private HostVmFilter filter;
    
    @Before
    public void setUp() throws Exception {
        filter = new HostVmFilter();
    }

    @After
    public void tearDown() throws Exception {
        filter = null;
    }

    @Test
    public void vmStringIDfilterMatches() {
        VmRef ref = mock(VmRef.class);
        when(ref.getStringID()).thenReturn("operation1");
        when(ref.getName()).thenReturn("noMatch");
        filter.setFilter("op");
        assertTrue(filter.matches(ref));
    }
    
    @Test
    public void vmNamefilterMatches() {
        VmRef ref = mock(VmRef.class);
        when(ref.getName()).thenReturn("operation1");
        when(ref.getStringID()).thenReturn("noMatch");
        filter.setFilter("op");
        assertTrue(filter.matches(ref));
    }
    
    @Test
    public void filterDoesntMatch() {
        VmRef ref = mock(VmRef.class);
        when(ref.getName()).thenReturn("test1");
        when(ref.getStringID()).thenReturn("test3");
        filter.setFilter("op");
        assertFalse(filter.matches(ref));
    }
    
    @Test
    public void filterMatches() {
        VmRef ref = mock(VmRef.class);
        when(ref.getName()).thenReturn("test1");
        when(ref.getStringID()).thenReturn("test1");
        filter.setFilter("test1");
        assertTrue(filter.matches(ref));
    }

}
