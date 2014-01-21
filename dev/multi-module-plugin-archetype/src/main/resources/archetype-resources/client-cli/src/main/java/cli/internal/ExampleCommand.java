package ${package}.cli.internal;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.HostInfo;
import ${package}.storage.ExampleDAO;

public class ExampleCommand extends AbstractCommand {

    public static final String NAME = "example-command";
    private static final String AGENT_ARG = "hostId";
    
    private final BundleContext context;
    
    ExampleCommand(BundleContext context) {
        this.context = context;
    }

    @Override
    public void run(CommandContext ctxt) throws CommandException {
        Arguments args = ctxt.getArguments();
        if (!args.hasArgument(AGENT_ARG)) {
            throw new CommandException(new LocalizedString("Agent argument required!"));
        }
        AgentInfoDAO agentInfo = getDaoService(AgentInfoDAO.class);
        if (agentInfo == null) {
            throw new CommandException(new LocalizedString("AgentInfoDAO unavaialable!"));
        }
        String agentId = args.getArgument(AGENT_ARG);
        
        // Use our DAO to resolve the agent, i.e. host
        HostRef hostRef = new HostRef(agentId, "not-used");
        AgentInformation resolvedAgent = agentInfo.getAgentInformation(hostRef);
        if (resolvedAgent == null) {
            throw new CommandException(new LocalizedString("Unknown agentId: " + agentId));
        }
        HostInfoDAO hostInfo = getDaoService(HostInfoDAO.class);
        if (hostInfo == null) {
            throw new CommandException(new LocalizedString("HostInfoDAO unavaialable!"));
        }
        HostInfo info = hostInfo.getHostInfo(hostRef);
        ExampleDAO exampleDAO = getDaoService(ExampleDAO.class);
        if (exampleDAO == null) {
            throw new CommandException(new LocalizedString("ExampleDAO unavailable"));
        }
        String message = exampleDAO.getMessage(hostRef);
        if (message == null) {
            message = "something's fishy :)";
        }
        // Here one should use appropriate formatters instead. Shame on me :(
        ctxt.getConsole().getOutput().println("Host: " + info.getHostname());
        ctxt.getConsole().getOutput().println("Message: " + message);
    }
    
    private <T> T getDaoService(Class<T> clazz) {
        ServiceReference ref = context.getServiceReference(clazz.getName());
        @SuppressWarnings("unchecked")
        T service = (T) context.getService(ref);
        return service;
    }
    
    @Override
    public boolean isStorageRequired() {
        // Let the launcher connect to storage for us
        return true;
    }

}
