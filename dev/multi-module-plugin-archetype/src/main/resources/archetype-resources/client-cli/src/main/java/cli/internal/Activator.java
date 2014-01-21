package ${package}.cli.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import com.redhat.thermostat.common.cli.CommandRegistry;
import com.redhat.thermostat.common.cli.CommandRegistryImpl;

/**
 * Registers the {@link ExampleCommand} with Thermostat.
 */
public class Activator implements BundleActivator {

    private CommandRegistry reg;
    
    @Override
    public void start(BundleContext context) throws Exception {    
        ExampleCommand cmd = new ExampleCommand(context);
        reg = new CommandRegistryImpl(context);
        reg.registerCommand(ExampleCommand.NAME, cmd);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (reg != null) {
            // unregisters commands which this registry registered
            reg.unregisterCommands();
        }
    }
}
