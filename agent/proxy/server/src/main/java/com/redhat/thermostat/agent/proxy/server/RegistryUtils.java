package com.redhat.thermostat.agent.proxy.server;

import java.net.InetAddress;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

class RegistryUtils {
    
    Registry getRegistry() throws RemoteException {
        return LocateRegistry.getRegistry(InetAddress.getLoopbackAddress().getHostName());
    }
    
    Remote exportObject(Remote obj) throws RemoteException {
        // Single arg method exports stub instead of real object
        return UnicastRemoteObject.exportObject(obj, 0);
    }
    
    void unexportObject(Remote obj) throws NoSuchObjectException {
        UnicastRemoteObject.unexportObject(obj, true);
    }
    
}