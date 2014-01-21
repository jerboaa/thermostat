package ${package}.storage.internal;

import java.util.Map;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.MultipleServiceTracker.Action;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.WriterID;
import ${package}.storage.ExampleDAO;

public class Activator implements BundleActivator {
    
    private MultipleServiceTracker multiTracker;

    @Override
    public void start(final BundleContext context) throws Exception {
        Class<?>[] dependentServices = new Class[] {
                Storage.class,
                WriterID.class
        };
        // Track Storage and WriterID and register our new DAO once services
        // become available.
        ServicesAvailableAction action = new ServicesAvailableAction(context);
        multiTracker = new MultipleServiceTracker(context,
                                                  dependentServices,
                                                  action);
        multiTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        multiTracker.close();
    }
    
    private static final class ServicesAvailableAction implements Action {

        private final BundleContext context;
        private ServiceRegistration reg;
        
        private ServicesAvailableAction(BundleContext context) {
            this.context = context;
        }
        
        @Override
        public void dependenciesAvailable(Map<String, Object> services) {
            Storage storage = (Storage)services.get(Storage.class.getName());
            WriterID writerId = (WriterID)services.get(WriterID.class.getName());
            ExampleDAO daoImpl = new ExampleDAOImpl(storage, writerId);
            reg = context.registerService(ExampleDAO.class.getName(), daoImpl, null);
        }

        @Override
        public void dependenciesUnavailable() {
            if (reg != null) {
                reg.unregister();
            }
        }
        
    }

}
