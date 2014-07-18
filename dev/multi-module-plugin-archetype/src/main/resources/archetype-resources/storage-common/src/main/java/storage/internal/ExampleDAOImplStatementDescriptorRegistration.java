package ${package}.storage.internal;

import java.util.HashSet;
import java.util.Set;

import ${package}.storage.ExampleDAO;

import com.redhat.thermostat.storage.core.PreparedParameter;
import com.redhat.thermostat.storage.core.auth.DescriptorMetadata;
import com.redhat.thermostat.storage.core.auth.StatementDescriptorRegistration;



public class ExampleDAOImplStatementDescriptorRegistration implements StatementDescriptorRegistration {

    private final Set<String> descs;

    public ExampleDAOImplStatementDescriptorRegistration() {
        descs = new HashSet<>(2);
        descs.add(ExampleDAOImpl.QUERY_DESCRIPTOR);
        descs.add(ExampleDAOImpl.REPLACE_DESCRIPTOR);
    }

    @Override
    public DescriptorMetadata getDescriptorMetadata(String descriptor,
            PreparedParameter[] params) {
        if (descs.contains(descriptor)) {
            String agentId = (String)params[0].getValue();
            DescriptorMetadata metadata = new DescriptorMetadata(agentId);
            return metadata;
        } else {
            throw new IllegalArgumentException("Unknown descriptor: ->"
                    + descriptor + "<-");
        }
    }

    @Override
    public Set<String> getStatementDescriptors() {
        return descs;
    }

}
