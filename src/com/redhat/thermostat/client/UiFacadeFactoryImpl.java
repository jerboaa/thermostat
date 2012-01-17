package com.redhat.thermostat.client;

public class UiFacadeFactoryImpl implements UiFacadeFactory {

    private MongoConnection connection;

    public UiFacadeFactoryImpl(MongoConnection connection) {
        this.connection = connection;
    }

    @Override
    public MainWindowFacade getMainWindow() {
        return new MainWindowFacadeImpl(connection.getDB());
    }

    @Override
    public SummaryPanelFacade getSummaryPanel() {
        return new SummaryPanelFacadeImpl(connection.getDB());

    }

    @Override
    public HostPanelFacade getHostPanel(HostRef ref) {
        return new HostPanelFacadeImpl(ref, connection.getDB());

    }

    @Override
    public VmPanelFacade getVmPanel(VmRef ref) {
        return new VmPanelFacadeImpl(ref, connection.getDB());

    }

}
