package com.redhat.thermostat.client;

public class DummyUiFacadeFactory implements UiFacadeFactory {

    private final DummyFacade dummyFacade;

    public DummyUiFacadeFactory() {
        dummyFacade = new DummyFacade();
    }

    @Override
    public MainWindowFacade getMainWindow() {
        return dummyFacade;
    }

    @Override
    public SummaryPanelFacade getSummaryPanel() {
        return dummyFacade;
    }

    @Override
    public HostPanelFacade getHostPanel(HostRef ref) {
        return dummyFacade;
    }

    @Override
    public VmPanelFacade getVmPanel(VmRef ref) {
        return dummyFacade;
    }

}
