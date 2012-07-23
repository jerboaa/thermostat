package com.redhat.thermostat.common;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.DefaultHostsVMsLoader;
import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.VmInfoDAO;
import com.redhat.thermostat.common.dao.VmRef;

public class DefaultHostsVMsLoaderTest {

    private HostInfoDAO mockHostsDAO;
    private VmInfoDAO mockVmsDAO;
    private DefaultHostsVMsLoader loader;
    
    @Before
    public void setUp() throws Exception {
        mockHostsDAO = mock(HostInfoDAO.class);
        mockVmsDAO = mock(VmInfoDAO.class);
    }

    @After
    public void tearDown() throws Exception {
        mockHostsDAO = null;
        mockVmsDAO = null;
        loader = null;
    }

    @Test
    public void canGetHosts() {
        Collection<HostRef> expectedHosts = new ArrayList<>();
        expectedHosts.add(new HostRef("123", "fluffhost1"));
        expectedHosts.add(new HostRef("456", "fluffhost2"));
        expectedHosts.add(new HostRef("007", "deadHost"));

        loader = new DefaultHostsVMsLoader(mockHostsDAO, mockVmsDAO, false);
        when(mockHostsDAO.getHosts()).thenReturn(expectedHosts);
        assertEquals(loader.getHosts(), expectedHosts);
        
        loader = new DefaultHostsVMsLoader(mockHostsDAO, mockVmsDAO, true);
        Collection<HostRef> aliveHosts = new ArrayList<>();
        expectedHosts.add(new HostRef("123", "fluffhost1"));
        expectedHosts.add(new HostRef("456", "fluffhost2"));
        when(mockHostsDAO.getAliveHosts()).thenReturn(aliveHosts);
        assertEquals(loader.getHosts(), aliveHosts);
    }
    
    @Test
    public void canGetVms() {
        HostRef hostR = mock(HostRef.class);
        Collection<VmRef> expectedVms = new ArrayList<>();
        expectedVms.add(new VmRef(hostR, 1, "test1"));
        expectedVms.add(new VmRef(hostR, 2, "test2"));

        loader = new DefaultHostsVMsLoader(mockHostsDAO, mockVmsDAO, false /* irrelevant */);
        when(mockVmsDAO.getVMs(hostR)).thenReturn(expectedVms);
        assertEquals(loader.getVMs(hostR), expectedVms);
        
        assertEquals(0, loader.getVMs(mock(HostRef.class)).size());
    }

}
