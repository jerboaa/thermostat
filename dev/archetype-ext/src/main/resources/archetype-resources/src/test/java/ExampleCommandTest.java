package ${package};

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.test.TestCommandContextFactory;

public class ExampleCommandTest {

    private TestCommandContextFactory cmdCtxFactory;
    private BundleContext bundleContext;
    private ExampleCommand cmd;
    
    @Before
    public void setUp() {
        Bundle sysBundle = mock(Bundle.class);
        bundleContext = mock(BundleContext.class);
        when(bundleContext.getBundle(0)).thenReturn(sysBundle);
        cmdCtxFactory = new TestCommandContextFactory(bundleContext);
        cmd = new ExampleCommand();
    }
    
    @After
    public void tearDown() {
        cmdCtxFactory = null;
        cmd = null;
    }
    
    @Test
    public void verifyCommandOutput() throws Exception {
        // TODO: Show something more useful
        fail("Implement Me!");
    }
}
