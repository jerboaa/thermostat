package com.redhat.thermostat.storage.mongodb.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.storage.core.StorageProvider;

public class Activator implements BundleActivator {

    @SuppressWarnings("rawtypes")
    private ServiceRegistration reg;
    
    @Override
    public void start(BundleContext context) throws Exception {
        StorageProvider prov = new MongoStorageProvider();
        reg = context.registerService(StorageProvider.class.getName(), prov, null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        reg.unregister();
    }

}
