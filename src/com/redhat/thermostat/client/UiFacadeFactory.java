package com.redhat.thermostat.client;

public interface UiFacadeFactory {

    public MainWindowFacade getMainWindow();

    public SummaryPanelFacade getSummaryPanel();

    public HostPanelFacade getHostPanel(HostRef ref);

    public VmPanelFacade getVmPanel(VmRef ref);

}
