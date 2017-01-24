/*
 * Copyright 2012-2017 Red Hat, Inc.
 *
 * This file is part of Thermostat.
 *
 * Thermostat is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your
 * option) any later version.
 *
 * Thermostat is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Thermostat; see the file COPYING.  If not see
 * <http://www.gnu.org/licenses/>.
 *
 * Linking this code with other modules is making a combined work
 * based on this code.  Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this code give
 * you permission to link this code with independent modules to
 * produce an executable, regardless of the license terms of these
 * independent modules, and to copy and distribute the resulting
 * executable under terms of your choice, provided that you also
 * meet, for each linked independent module, the terms and conditions
 * of the license of that module.  An independent module is a module
 * which is not derived from or based on this code.  If you modify
 * this code, you may extend this exception to your version of the
 * library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.redhat.thermostat.client.swing.internal.views;

import java.awt.BorderLayout;
import java.awt.Component;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JPanel;

import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.client.core.views.UIPluginInfo;
import com.redhat.thermostat.client.core.views.VmInformationView;
import com.redhat.thermostat.client.swing.EdtHelper;
import com.redhat.thermostat.client.swing.OverlayContainer;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.internal.Tab;
import com.redhat.thermostat.client.swing.internal.TabbedPane;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.locale.LocalizedString;

public class VmInformationPanel extends VmInformationView implements SwingComponent {
    private static final String VIEW_NAME = "%VIEW_NAME%";
    private static final String TEMPLATE =
            "There's a non-swing view registered: '" + VIEW_NAME + "'. " +
            "The swing client can not use these views. This is "         +
            "most likely a developer mistake. If this is meant to "      +
            "be a swing-based view, it must implement the "              +
            "'SwingComponent' interface. If it's not meant to be a "     +
            "swing-based view, it should not have been registered.";

    private static final Logger logger = LoggingUtils.getLogger(VmInformationPanel.class);
    private static final EdtHelper edtHelper = new EdtHelper();

    private final TabbedPane tabPane = new TabbedPane();
    private JPanel visiblePanel;

    public VmInformationPanel() {
        super();
        visiblePanel = new JPanel();
        visiblePanel.setLayout(new BorderLayout());
        tabPane.setName("tabPane");
        visiblePanel.add(tabPane);
    }

    TabbedPane __test__getTabPane() {
        return tabPane;
    }

    @Override
    public void clear() {
        try {
            edtHelper.callAndWait(new Runnable() {
                @Override
                public void run() {
                    tabPane.removeAll();
                }
            });

        } catch (InvocationTargetException | InterruptedException e) {
            logger.severe(e.getLocalizedMessage());
        }
    }

    private Tab makeTab(SwingComponent component, LocalizedString title) {
        Tab tab = null;

        JComponent tabContent = null;
        Component comp = component.getUiComponent();
        if (comp instanceof JComponent) {
            tabContent = (JComponent) comp;
        } else {
            tabContent = new JPanel();
            tabContent.setLayout(new BorderLayout());
            tabContent.add(comp);
        }

        tab = new Tab(tabContent, title);
        return tab;
    }

    @Override
    public void addChildViews(final List<UIPluginInfo> plugins) {
        try {
            edtHelper.callAndWait(new Runnable() {
                @Override
                public void run() {
                    for (UIPluginInfo plugin : plugins) {
                        UIComponent view = plugin.getView();
                        if (view instanceof SwingComponent) {
                            addViewImpl(plugin.getLocalizedName(), (SwingComponent) view);
                        } else {
                            logger.severe(getLoggerMessage(view));
                        }
                    }
                }
            });
        } catch (InvocationTargetException | InterruptedException e) {
            logger.severe(e.getLocalizedMessage());
        }
    }

    String getLoggerMessage(UIComponent view) {
        return TEMPLATE.replace(VIEW_NAME, view.toString());
    }

    private void addViewImpl(final LocalizedString title, final SwingComponent view) {
        Tab tabContent = makeTab(view, title);
        tabPane.add(tabContent);
        if (view instanceof OverlayContainer) {
            OverlayContainer overlayContainer = (OverlayContainer) view;
            tabPane.addMouseListener(overlayContainer.getOverlay().getClickOutCloseListener(tabPane));
        }
    }

    @Override
    public void addChildView(final LocalizedString title, final UIComponent view) {
        if (view instanceof SwingComponent) {
            try {
                edtHelper.callAndWait(new Runnable() {
                    @Override
                    public void run() {
                        addViewImpl(title, (SwingComponent) view);
                    }
                });
            } catch (InvocationTargetException | InterruptedException e) {
                logger.severe(e.getLocalizedMessage());
            }
        } else {
            String message = getLoggerMessage(view);
            logger.severe(message);
            throw new AssertionError(message);
        }
    }

    public Component getUiComponent() {
        return visiblePanel;
    }

    @Override
    public int getSelectedChildID() {
        FutureTask<Integer> task = new FutureTask<>(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return tabPane.getSelectedIndex();
            }
        });

        try {
            edtHelper.callAndWait(task);
            return task.get();

        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public boolean selectChildID(final int id) {
        FutureTask<Boolean> task = new FutureTask<>(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                if (tabPane.getTabs().size() > id) {
                    tabPane.setSelectedIndex(id);
                    return true;
                }
                return false;
            }
        });

        try {
            edtHelper.callAndWait(task);
            return task.get();

        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int getNumChildren() {
        FutureTask<Integer> task = new FutureTask<>(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return tabPane.getTabs().size();
            }
        });

        try {
            edtHelper.callAndWait(task);
            return task.get();

        } catch (Exception e) {
            return 0;
        }
    }
}

