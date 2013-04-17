package com.redhat.thermostat.launcher.internal;

import java.util.Arrays;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.CommandRegistryImpl;
import com.redhat.thermostat.common.utils.LoggingUtils;

/**
 * Provides {@link Command} and {@link CommandInfo} objects for a given command
 * name.
 *
 * @see CommandRegistryImpl
 */
public class CommandSource {

    private static final Logger logger = LoggingUtils.getLogger(CommandSource.class);

    private final BundleContext context;

    public CommandSource(BundleContext context) {
        this.context = context;
    }

    public Command getCommand(String name) {
        try {
            ServiceReference[] refs = context.getAllServiceReferences(Command.class.getName(), "(" + Command.NAME + "=" + name + ")");
            if (refs == null) {
                return null;
            } else if (refs.length > 1) {
                logger.warning("More than one matching command implementation found for " + name);
                for (int i = 0; i < refs.length; i++) {
                    logger.warning(name + " is provided by + " + Arrays.toString((String[])refs[i].getProperty(Constants.OBJECTCLASS)));
                }
            }
            ServiceReference ref = refs[0];
            return (Command) context.getService(ref);
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("bad name for command: " + name, e);
        }
    }

}
