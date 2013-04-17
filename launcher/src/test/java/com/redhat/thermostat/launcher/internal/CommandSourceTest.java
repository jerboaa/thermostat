package com.redhat.thermostat.launcher.internal;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import java.util.Hashtable;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.testutils.StubBundleContext;

public class CommandSourceTest {

    private CommandSource commandSource;

    private StubBundleContext bundleContext;
    private CommandInfoSource infoSource;

    @Before
    public void setUp() {
        bundleContext = new StubBundleContext();
        commandSource = new CommandSource(bundleContext);

        infoSource = mock(CommandInfoSource.class);

        bundleContext.registerService(CommandInfoSource.class, infoSource, null);
    }

    @Test
    public void testGetNotRegisteredCommand() throws InvalidSyntaxException {
        Command result = commandSource.getCommand("test1");

        assertNull(result);
    }

    @Test
    public void testGetCommandAndInfo() throws InvalidSyntaxException {
        Command cmd = mock(Command.class);
        registerCommand("test", cmd);

        Command result = commandSource.getCommand("test1");

        assertSame(cmd, result);
    }

    @Test
    public void testDoubleRegisteredCommand() throws InvalidSyntaxException {
        Command cmd1 = mock(Command.class);
        Command cmd2 = mock(Command.class);

        registerCommand("test1", cmd1);
        registerCommand("test1", cmd2);

        Command cmd = commandSource.getCommand("test1");

        assertSame(cmd1, cmd);
    }

    private ServiceRegistration registerCommand(String name, Command cmd) {
        Hashtable<String,String> props = new Hashtable<>();
        props.put(Command.NAME, "test1");
        return bundleContext.registerService(Command.class, cmd, props);
    }

 }
