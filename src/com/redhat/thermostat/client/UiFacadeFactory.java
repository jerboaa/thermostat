package com.redhat.thermostat.client;

public class UiFacadeFactory {

    private static Connection connection;

    private static DummyFacade dummy;

    public static MainWindowFacade getMainWindow() {
        return getDummy();
    }

    public static SummaryPanelFacade getSummaryPanel() {
        return getDummy();
    }

    public static HostPanelFacade getHostPanel(HostRef ref) {
        return getDummy();
    }

    public static VmPanelFacade getVmPanel(VmRef ref) {
        return getDummy();
    }

    public static void setConnection(Connection connection) {
        UiFacadeFactory.connection = connection;
    }

    private static DummyFacade getDummy() {
        if (dummy == null) {
            dummy = new DummyFacade();
        }
        return dummy;
    }

}
