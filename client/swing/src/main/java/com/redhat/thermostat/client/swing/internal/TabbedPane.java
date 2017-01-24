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

package com.redhat.thermostat.client.swing.internal;

import com.redhat.thermostat.client.swing.UIDefaults;
import com.redhat.thermostat.client.swing.components.FontAwesomeIcon;
import com.redhat.thermostat.client.swing.components.GradientPanel;
import com.redhat.thermostat.client.swing.components.ThermostatPopupMenu;
import com.redhat.thermostat.client.swing.internal.vmlist.UIDefaultsImpl;
import com.redhat.thermostat.client.ui.Palette;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class TabbedPane extends JComponent {

    public static final String TABBED_PANE_ID = "_TabbedPane_";
    public static final String CONTROLS_ID = "_TabbedPane_Controls_";
    public static final String CONTENT_ID = "_TabbedPane_Content_";

    private GradientPanel controls;
    private JPanel contentPane;

    private List<Tab> tabs;
    private Map<String, TabUI> hiddenControls;

    private CardLayout contentPaneLayout;

    private TabController controller;

    private JLabel hiddenTabsControl;
    private UIDefaults defaults = UIDefaultsImpl.getInstance();

    @Override
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }

    public TabbedPane() {

        setName(TABBED_PANE_ID);

        controller = new TabController();

        tabs = new ArrayList<>();
        hiddenControls = new HashMap<>();

        setLayout(new BorderLayout());

        this.controls = new GradientPanel(Color.WHITE, Palette.LIGHT_GRAY.getColor());
        controls.setLayout(new TabbedPaneControlPanelLayout());
        controls.setName(CONTROLS_ID);

        hiddenTabsControl = new JLabel(new FontAwesomeIcon('\uf0d7', 12, defaults.getSelectedComponentBGColor()));
        hiddenTabsControl.setHorizontalAlignment(SwingConstants.CENTER);
        hiddenTabsControl.setVerticalAlignment(SwingConstants.CENTER);
        hiddenTabsControl.addMouseListener(controller.getHiddenTabsListener());
        controls.add(hiddenTabsControl);

        add(controls, BorderLayout.NORTH);

        contentPaneLayout = new CardLayout();

        this.contentPane = new JPanel();
        this.contentPane.setBackground(Color.WHITE);
        this.contentPane.setLayout(contentPaneLayout);
        contentPane.setName(CONTENT_ID);

        add(contentPane, BorderLayout.CENTER);
    }

    public void add(Tab content) {
        add(content, tabs.size());
    }

    @Override
    public void removeAll() {
        controls.removeAll();
        controls.add(hiddenTabsControl);

        contentPane.removeAll();

        for (Tab tab : tabs) {
            tab.removeMouseListener(controller.getListener());
            tab.removeMouseMotionListener(controller.getListener());
        }
        tabs.clear();
        hiddenControls.clear();

        revalidate();
        repaint();
    }

    public void add(Tab content, int position) {
        addDeferValidation(content, position);
        setSelectedIndex(getSelectedIndex());

        revalidate();
        repaint();
    }

    private void addDeferValidation(Tab tab, int position) {
        tabs.add(position, tab);

        tab.addMouseListener(controller.getListener());
        tab.addMouseMotionListener(controller.getListener());

        TabUI hiddenControl = new TabUI(tab.getTabName());
        hiddenControl.addMouseListener(controller.getListener());
        hiddenControl.addMouseMotionListener(controller.getListener());

        hiddenControls.put(tab.getTabName().getContents(), hiddenControl);

        controls.add(tab);
        contentPane.add(tab.getContent(), "" + position);
    }

    public void add(List<Tab> tabs) {
        int position = tabs.size();
        for (Tab tab : tabs) {
            addDeferValidation(tab, position++);
        }

        revalidate();
        repaint();
    }

    public void remove(Tab tab) {

        tabs.remove(tab);
        contentPane.remove(tab);
        controls.remove(tab.getContent());

        tab.removeMouseListener(controller.getListener());
        tab.removeMouseMotionListener(controller.getListener());

        TabUI hiddenControl = hiddenControls.remove(tab.getTabName().getContents());
        hiddenControl.removeMouseListener(controller.getListener());
        hiddenControl.removeMouseMotionListener(controller.getListener());

        revalidate();
        repaint();
    }

    public List<Tab> getTabs() {
        return tabs;
    }

    public int getSelectedIndex() {
        return controller.getSelected();
    }

    public void setSelectedIndex(int index) {
        this.controller.setSelected(index);
    }

    TabController getController() {
        return controller;
    }

    JComponent getHiddenTabsControl() {
        return hiddenTabsControl;
    }

    class TabController {

        private List<Tab> hiddenTabs;
        private int selected;

        private MouseAdapter adapter;
        private MouseAdapter hiddenTabsAdapter;
        private ThermostatPopupMenu menu;

        public TabController() {

            menu = new ThermostatPopupMenu();
            hiddenTabs = new ArrayList<>();

            selected = -1;

            adapter = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {

                    menu.setVisible(false);

                    TabUI selectedTab = (TabUI) e.getSource();
                    String name = selectedTab.getTabName().getContents();

                    for (TabUI tab : hiddenControls.values()) {
                        if (name.equals(tab.getTabName().getContents())) {
                            tab.getModel().setHover(false);
                            tab.getModel().setSelected(true);
                            tab.repaint();
                            continue;
                        }
                        tab.getModel().setHover(false);
                        tab.getModel().setSelected(false);
                        tab.repaint();
                    }

                    int index = 0;

                    for (TabUI tab : tabs) {
                        if (name.equals(tab.getTabName().getContents())) {
                            tab.getModel().setHover(false);
                            tab.getModel().setSelected(true);
                            tab.repaint();

                            selected = index;
                            contentPaneLayout.show(contentPane, "" + index);
                            continue;
                        }
                        index++;
                        tab.getModel().setHover(false);
                        tab.getModel().setSelected(false);
                        tab.repaint();
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    TabUI hoveredTab = (TabUI) e.getSource();
                    hoveredTab.getModel().setHover(true);
                    hoveredTab.repaint();

                    for (TabUI tab : tabs) {
                        if (hoveredTab.equals(tab)) {
                            continue;
                        }
                        tab.getModel().setHover(false);
                        tab.repaint();
                    }
                    for (TabUI tab : hiddenControls.values()) {
                        if (hoveredTab.equals(tab)) {
                            continue;
                        }
                        tab.getModel().setHover(false);
                        tab.repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    TabUI tab = (TabUI) e.getSource();
                    tab.getModel().setHover(false);
                    tab.repaint();
                }
            };

            hiddenTabsAdapter = new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    JPanel panel = new JPanel();
                    panel.setLayout(new GridLayout(0, 1));
                    for (Tab tab : hiddenTabs) {
                        panel.add(hiddenControls.get(tab.getTabName().getContents()));
                    }
                    menu.removeAll();
                    menu.add(panel);
                    menu.show(hiddenTabsControl, e.getX(), e.getY());
                }
            };
        }

        public List<Tab> getHiddenTabs() {
            return hiddenTabs;
        }

        public void setSelected(int selected) {

            if (tabs.isEmpty()) {
                return;
            }

            if (selected < 0) {
                selected = 0;
            }

            int index = 0;
            this.selected = selected;
            for (Tab tab : tabs) {
                if (index == selected) {
                    tab.getModel().setHover(false);
                    tab.getModel().setSelected(true);
                    tab.repaint();
                    contentPaneLayout.show(contentPane, "" + index);
                } else {
                    tab.getModel().setHover(false);
                    tab.getModel().setSelected(false);
                    tab.repaint();
                }
                index++;
            }
            revalidate();
            repaint();
        }

        public int getSelected() {
            return selected;
        }

        public MouseAdapter getListener() {
            return adapter;
        }

        public MouseAdapter getHiddenTabsListener() {
            return hiddenTabsAdapter;
        }
    }
}
