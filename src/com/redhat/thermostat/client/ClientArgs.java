package com.redhat.thermostat.client;

import java.awt.Window;

import com.redhat.thermostat.client.ui.LayoutDebugHelper;

public class ClientArgs {

    private static boolean isDebugLayout =
            Boolean.getBoolean("thermostat.debug-layout");

    // private static boolean isDebugLayout = true;

    public ClientArgs(String[] initialArgs) {
        // remove 'unused' warnings
        for (String arg : initialArgs) {
            if (arg.equals("--debug-layout")) {
                isDebugLayout = true;
            }
        }
        // TODO what arguments do we care about?
        // perhaps skipping the mode selection?

        if (isDebugLayout()) {
            Thread layoutDebugger = new Thread(new Runnable() {
                @Override
                public void run() {
                    LayoutDebugHelper helper = new LayoutDebugHelper();
                    while (true) {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            System.err.println("Layout Debug Helper exiting");
                        }
                        Window[] windows = Window.getWindows();
                        for (Window w : windows) {
                            helper.debugLayout(w);
                            w.invalidate();
                            w.repaint();
                        }
                    }
                }
            });
            layoutDebugger.start();
        }
    }

    public static boolean isDebugLayout() {
        return isDebugLayout;
    }
}
