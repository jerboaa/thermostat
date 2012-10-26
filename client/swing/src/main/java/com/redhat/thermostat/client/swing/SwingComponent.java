package com.redhat.thermostat.client.swing;

import java.awt.Component;

import com.redhat.thermostat.client.core.views.UIComponent;

public interface SwingComponent extends UIComponent {

    Component getUiComponent();
}
