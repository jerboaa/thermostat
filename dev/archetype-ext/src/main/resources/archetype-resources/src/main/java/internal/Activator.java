package ${package}.internal;

import java.util.Hashtable;

import ${package}.ExampleCommand;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import com.redhat.thermostat.common.cli.Command;
/**
 * Stub Activator
 */
public class Activator implements BundleActivator {

    private ServiceRegistration registration;

    @Override
    public void start(BundleContext context) throws Exception {    
        // Activate your bundle here
        Hashtable<String,String> properties = new Hashtable<>();
        properties.put(Command.NAME, "example-command");
        registration = context.registerService(Command.class.getName(), new ExampleCommand(), properties);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // unregister services here
        registration.unregister();
    }
}
