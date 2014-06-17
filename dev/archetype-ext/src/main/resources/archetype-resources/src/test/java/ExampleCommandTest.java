package ${package};

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.test.TestCommandContextFactory;

public class ExampleCommandTest {

    private TestCommandContextFactory cmdCtxFactory;
    private BundleContext bundleContext;
    private ExampleCommand cmd;
    private CommandContext ctxt;
    private Arguments mockArgs;
    private Console console;
    private ByteArrayOutputStream outputBaos, errorBaos;
    private PrintStream output, error;
    
    @Before
    public void setUp() {
        Bundle sysBundle = mock(Bundle.class);
        bundleContext = mock(BundleContext.class);
        when(bundleContext.getBundle(0)).thenReturn(sysBundle);
        cmdCtxFactory = new TestCommandContextFactory(bundleContext);
        cmd = new ExampleCommand();
        ctxt = mock(CommandContext.class);
        mockArgs = mock(Arguments.class);
        console = mock(Console.class);

        outputBaos = new ByteArrayOutputStream();
        output = new PrintStream(outputBaos);

        errorBaos = new ByteArrayOutputStream();
        error = new PrintStream(errorBaos);

        when(ctxt.getArguments()).thenReturn(mockArgs);
        when(ctxt.getConsole()).thenReturn(console);
        when(console.getOutput()).thenReturn(output);
        when(console.getError()).thenReturn(error);
    }
    
    @After
    public void tearDown() {
        cmdCtxFactory = null;
        cmd = null;
    }
    
    @Test
    public void verifyCommandOutput() throws CommandException {
        // TODO: Show something more useful
        cmd.run(ctxt);
        String commandOutput = new String(outputBaos.toByteArray());
        assertEquals("Hello World!\n",commandOutput);
    }
}
