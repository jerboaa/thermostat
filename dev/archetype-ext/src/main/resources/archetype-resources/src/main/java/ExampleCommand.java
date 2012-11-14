package ${package};

import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleCommand;

public class ExampleCommand extends SimpleCommand {

    public ExampleCommand() {
        // Nothing
    }
    
    @Override
    public void run(CommandContext ctx) throws CommandException {
        // FIXME: Do something useful :)
        ctx.getConsole().getOutput().println("Hello World!");
    }

    @Override
    public String getName() {
        return "examplecommand";
    }

    @Override
    public boolean isStorageRequired() {
        return false;
    }

}
