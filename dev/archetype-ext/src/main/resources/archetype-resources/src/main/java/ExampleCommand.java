package ${package};

import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;

public class ExampleCommand implements Command {

    public ExampleCommand() {
        // Nothing
    }
    
    @Override
    public void run(CommandContext ctx) throws CommandException {
        // FIXME: Do something useful :)
        ctx.getConsole().getOutput().println("Hello World!");
    }

    @Override
    public boolean isStorageRequired() {
        return false;
    }

}