package com.redhat.thermostat.storage.core;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.storage.config.StartupConfiguration;

public class StorageProviderUtil {

    // FIXME: This should go away once GuiClientCommand and AgentApplication use
    // DbService via launcher for establishing a connection. I.e. those should just
    // specify isStorageRequired() == true and the launcher handles the rest
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static StorageProvider getStorageProvider(StartupConfiguration config) {
        Bundle bundle = FrameworkUtil.getBundle(StorageProviderUtil.class);
        BundleContext ctxt = bundle.getBundleContext();
        try {
            ServiceReference[] refs = ctxt.getServiceReferences(StorageProvider.class.getName(), null);
            for (int i = 0; i < refs.length; i++) {
                StorageProvider prov = (StorageProvider) ctxt.getService(refs[i]);
                prov.setConfig(config);
                if (prov.canHandleProtocol()) {
                    return prov;
                }
            }
        } catch (InvalidSyntaxException e) {
            // This should not happen since we use a null filter
            throw new AssertionError();
        }
        return null;
    }
}
