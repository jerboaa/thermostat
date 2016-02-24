/*
 * Copyright 2012-2016 Red Hat, Inc.
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeNode;

import com.redhat.thermostat.shared.config.CommonPaths;
import sun.misc.Signal;

import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.client.swing.EdtHelper;
import com.redhat.thermostat.client.swing.MenuHelper;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.OverlayPanel;
import com.redhat.thermostat.client.swing.internal.accordion.Accordion;
import com.redhat.thermostat.client.swing.internal.components.ThermostatGlassPane;
import com.redhat.thermostat.client.swing.internal.components.ThermostatGlassPaneLayout;
import com.redhat.thermostat.client.swing.internal.progress.AggregateNotificationPanel;
import com.redhat.thermostat.client.swing.internal.progress.ProgressNotificationArea;
import com.redhat.thermostat.client.swing.internal.progress.SwingProgressNotifier;
import com.redhat.thermostat.client.swing.internal.progress.SwingProgressNotifier.PropertyChange;
import com.redhat.thermostat.client.swing.internal.search.ReferenceFieldSearchFilter;
import com.redhat.thermostat.client.swing.internal.search.SearchField;
import com.redhat.thermostat.client.swing.internal.sidepane.ExpanderComponent;
import com.redhat.thermostat.client.swing.internal.sidepane.ThermostatSidePanel;
import com.redhat.thermostat.client.swing.internal.splitpane.ThermostatSplitPane;
import com.redhat.thermostat.client.swing.internal.vmlist.HostTreeComponentFactory;
import com.redhat.thermostat.client.swing.internal.vmlist.controller.ContextActionController;
import com.redhat.thermostat.client.swing.internal.vmlist.controller.DecoratorManager;
import com.redhat.thermostat.client.swing.internal.vmlist.controller.HostTreeController;
import com.redhat.thermostat.client.ui.MenuAction;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.utils.StringUtils;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;

@SuppressWarnings({ "restriction", "serial" })
public class MainWindow extends JFrame implements MainView {
    
    public static final String MAIN_WINDOW_NAME = "Thermostat_mainWindo_JFrame_parent#1";

    private static final Logger logger = LoggingUtils.getLogger(MainWindow.class);

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private SwingProgressNotifier notifier;

    private CommonPaths commonPaths;
    private final JMenuBar mainMenuBar = new JMenuBar();
    private MenuHelper mainMenuHelper;
    private JPanel contentArea = null;
    
    private final ShutdownClient shutdownAction;

    private ActionNotifier<Action> actionNotifier = new ActionNotifier<>(this);

    private StatusBar statusBar;
    
    private ThermostatSidePanel navigationPanel;
    private Accordion<HostRef, VmRef> hostTree;
    
    private HostTreeController hostTreeController;
    private ContextActionController contextActionController;

    private ReferenceFieldSearchFilter filter;

    public MainWindow() {
        super();

        setName(MAIN_WINDOW_NAME);

        shutdownAction = new ShutdownClient();

        contentArea = new JPanel(new BorderLayout());

        ThermostatGlassPane glassPane = new ThermostatGlassPane();
        glassPane.setLayout(new ThermostatGlassPaneLayout());

        setGlassPane(glassPane);

        setupMenus();
        setupPanels(glassPane);

        this.setPreferredSize(new Dimension(800, 600));
        
        statusBar = new StatusBar();
        setupNotificationPane(statusBar, glassPane);
        
        getContentPane().add(statusBar, BorderLayout.SOUTH);
        
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(shutdownAction);

        // Handle SIGTERM/SIGINT properly
        Signal.handle(new Signal("TERM"), shutdownAction);
        Signal.handle(new Signal("INT"), shutdownAction);
        
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                fireViewAction(Action.VISIBLE);
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                fireViewAction(Action.HIDDEN);
            }
        });
    }

    private void setupNotificationPane(StatusBar statusBar, final ThermostatGlassPane glassPane) {

        AggregateNotificationPanel aggregateNotificationArea = new AggregateNotificationPanel();
        
        ProgressNotificationArea notificationArea = new ProgressNotificationArea();
        notifier = new SwingProgressNotifier(aggregateNotificationArea, notificationArea);

        statusBar.add(notificationArea, BorderLayout.CENTER);

        LocalizedString title = translator.localize(LocaleResources.PROGRESS_NOTIFICATION_AREA_TITLE);
        final OverlayPanel overlay = new OverlayPanel(title, false);
        glassPane.add(overlay);
        glassPane.addMouseListener(overlay.getClickOutCloseListener(glassPane));
        overlay.addCloseEventListener(new OverlayPanel.CloseEventListener() {
            @Override
            public void closeRequested(OverlayPanel.CloseEvent event) {
                event.getSource().setOverlayVisible(false);
            }
        });

        overlay.add(aggregateNotificationArea, BorderLayout.CENTER);
        notifier.addPropertyChangeListener(new com.redhat.thermostat.common.
                ActionListener<SwingProgressNotifier.PropertyChange>() {
            @Override
            public void actionPerformed(com.redhat.thermostat.common.
                                                ActionEvent<PropertyChange> actionEvent) {
                overlay.setOverlayVisible(false);
            }
        });
        
        statusBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (notifier.hasTasks()) {
                    // ensure glasspane is visible
                    glassPane.setVisible(true);
                    overlay.setOverlayVisible(true);
                }
            }
        });
    }
    
    @Override
    public SwingProgressNotifier getNotifier() {
        return notifier;
    }
    
    private void setupMenus() {

        JMenu fileMenu = new JMenu(translator.localize(LocaleResources.MENU_FILE).getContents());
        fileMenu.getPopupMenu().setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        mainMenuBar.add(fileMenu);

        JMenuItem fileExitMenu = new JMenuItem(translator.localize(LocaleResources.MENU_FILE_EXIT).getContents());
        fileExitMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        fileExitMenu.addActionListener(shutdownAction);
        fileMenu.add(fileExitMenu);

        JMenu editMenu = new JMenu(translator.localize(LocaleResources.MENU_EDIT).getContents());
        mainMenuBar.add(editMenu);

        JMenuItem configureClientMenuItem = new JMenuItem(translator.localize(LocaleResources.MENU_EDIT_CONFIGURE_CLIENT).getContents());
        configureClientMenuItem.setName("showClientConfig");
        configureClientMenuItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                fireViewAction(Action.SHOW_CLIENT_CONFIG);
            }
        });
        editMenu.add(configureClientMenuItem);

        JMenu viewMenu = new JMenu(translator.localize(LocaleResources.MENU_VIEW).getContents());
        mainMenuBar.add(viewMenu);
        JMenuItem configureAgentMenuItem = new JMenuItem(translator.localize(LocaleResources.MENU_VIEW_AGENTS).getContents());
        configureAgentMenuItem.setName("showAgentConfig");
        configureAgentMenuItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                fireViewAction(Action.SHOW_AGENT_CONFIG);
            }
        });
        viewMenu.add(configureAgentMenuItem);

        JMenu helpMenu = new JMenu(translator.localize(LocaleResources.MENU_HELP).getContents());
        helpMenu.getPopupMenu().setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        mainMenuBar.add(helpMenu);

        JMenuItem helpUserGuideMenu = new JMenuItem(translator.localize(LocaleResources.MENU_HELP_USER_GUIDE).getContents());
        helpUserGuideMenu.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fireViewAction(Action.SHOW_USER_GUIDE);
            }
        });
        helpUserGuideMenu.setName("showUserGuide");
        helpMenu.add(helpUserGuideMenu);

        JMenuItem helpAboutMenu = new JMenuItem(translator.localize(LocaleResources.MENU_HELP_ABOUT).getContents());
        helpAboutMenu.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                fireViewAction(Action.SHOW_ABOUT_DIALOG);
            }
        });
        helpMenu.add(helpAboutMenu);
        setJMenuBar(mainMenuBar);
    }

    private void setupPanels(final ThermostatGlassPane glassPane) {
        final ThermostatSplitPane splitPane = new ThermostatSplitPane();
        splitPane.setOneTouchExpandable(false);
        getContentPane().add(splitPane);

        final JPanel detailsPanel = createDetailsPanel();
        detailsPanel.setMinimumSize(new Dimension(500, 500));
        splitPane.setRightComponent(detailsPanel);
        
        navigationPanel = new ThermostatSidePanel();
        splitPane.setLeftComponent(navigationPanel);
        
        DecoratorManager decoratorManager = new DecoratorManager();
        contextActionController = new ContextActionController();
        
        HostTreeComponentFactory hostFactory =
                new HostTreeComponentFactory(decoratorManager, contextActionController);
        hostTree = new Accordion<>(hostFactory);
        hostTreeController = new HostTreeController(hostTree, decoratorManager,
                                                    hostFactory);
        navigationPanel.addContent(hostTree);
        
        final JPanel collapsedPanel = new JPanel();
        collapsedPanel.setLayout(new BorderLayout());
        
        final ExpanderComponent expander = new ExpanderComponent();            
                        
        navigationPanel.setMinimumSize(new Dimension(300, 500));
        navigationPanel.addPropertyChangeListener(ThermostatSidePanel.COLLAPSED,
                                                  new PropertyChangeListener()
        {
            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (((Boolean) evt.getNewValue()).booleanValue()) {
                            getContentPane().remove(splitPane);
                            
                            collapsedPanel.add(expander, BorderLayout.WEST);
                            collapsedPanel.add(detailsPanel, BorderLayout.CENTER);    
                            
                            getContentPane().add(collapsedPanel);
                            revalidate();
                            repaint();
                        }
                    }
                });
            }
        });
        
        expander.addPropertyChangeListener(ExpanderComponent.EXPANDED_PROPERTY,
                                           new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (((Boolean) evt.getNewValue()).booleanValue()) {
                    
                    collapsedPanel.removeAll();
                    
                    getContentPane().remove(collapsedPanel);
                    
                    splitPane.setRightComponent(detailsPanel);
                    splitPane.setLeftComponent(navigationPanel);
                    
                    getContentPane().add(splitPane);
                    revalidate();
                    repaint();
                }
            }
        });

        installGlobalNavigation();
        installSearchFiled();
    }
    
    private void installGlobalNavigation() {
        JButton action = new JButton(translator.localize(LocaleResources.SHOW_ISSUES).getContents());
        action.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fireViewAction(Action.SHOW_ISSUES);
            }
        });
        navigationPanel.addContent(action, BorderLayout.SOUTH);
    }

    private void installSearchFiled() {
        // install the search field in the sidepane for now
        SearchField searchField = new SearchField();
        navigationPanel.getTopPane().add(searchField);
        
        filter = new ReferenceFieldSearchFilter(searchField, hostTreeController);
        hostTreeController.addFilter(filter);
    }
    
    private JPanel createDetailsPanel() {
        JPanel result = new JPanel(new BorderLayout());
        result.add(contentArea, BorderLayout.CENTER);
        return result;
    }

    public class ShutdownClient extends WindowAdapter implements java.awt.event.ActionListener, sun.misc.SignalHandler {

        @Override
        public void windowClosing(WindowEvent e) {
            shutdown();
        }

        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            shutdown();
        }
        
        @Override
        public void handle(Signal arg0) {
            shutdown();
        }

        private void shutdown() {
            dispose();
            mainMenuHelper.saveMenuStates();
            fireViewAction(Action.SHUTDOWN);
        }

    }

    @Override
    public JFrame getTopFrame() {
        return this;
    }
    
    @Override
    public void addActionListener(ActionListener<Action> l) {
        actionNotifier.addActionListener(l);
    }

    public void removeViewActionListener(ActionListener<Action> l) {
        actionNotifier.removeActionListener(l);
    }

    private void fireViewAction(Action action) {
        actionNotifier.fireAction(action);
    }
        
    @SuppressWarnings("unused") // Used for debugging but not in production code.
    private static void printTree(PrintStream out, TreeNode node, int depth) {
        out.println(StringUtils.repeat("  ", depth) + node.toString());
        @SuppressWarnings("unchecked")
        List<? extends TreeNode> children = Collections.list(node.children());
        for (TreeNode child : children) {
            printTree(out, child, depth + 1);
        }
    }

    @Override
    public void setWindowTitle(String title) {
        setTitle(title);
    }

    @Override
    public void showMainWindow() {
        try {
            new EdtHelper().callAndWait(new Runnable() {

                @Override
                public void run() {
                    pack();
                    setVisible(true);
                }
            });
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void hideMainWindow() {
        setVisible(false);
        dispose();
    }

    @Override
    public void setStatusBarPrimaryStatus(final LocalizedString primaryStatus) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                statusBar.setPrimaryStatus(primaryStatus);
            }
        });
    }
    
    @Override
    public void setSubView(final BasicView view) {
        if (view instanceof SwingComponent) {
            final SwingComponent swingComp = (SwingComponent)view;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    contentArea.removeAll();
                    Component toAdd = swingComp.getUiComponent();
                    contentArea.add(toAdd);
                    contentArea.revalidate();
                    contentArea.repaint();
                }
            });
        } else {
            String message = ""
                    + "There's a non-swing view registered: '" + view.toString()
                    + "'. The swing client can not use these views. This is "
                    + "most likely a developer mistake. If this is meant to "
                    + "be a swing-based view, it must implement the "
                    + "'SwingComponent' interface. If it's not meant to be a "
                    + "swing-based view, it should not have been registered.";
            logger.severe(message);
            throw new AssertionError(message);
        }
    }

    @Override
    public void setCommonPaths(CommonPaths commonPaths) {
        if (mainMenuHelper == null) {
            mainMenuHelper = new MenuHelper(commonPaths, mainMenuBar);
        }
        this.commonPaths = commonPaths;
    }

    @Override
    public void addMenu(MenuAction action) {
        mainMenuHelper.addMenuAction(action);
    }

    @Override
    public void removeMenu(MenuAction action) {
        mainMenuHelper.removeMenuAction(action);
    }

    @Override
    public HostTreeController getHostTreeController() {
        return hostTreeController;
    }

    @Override
    public ContextActionController getContextActionController() {
        return contextActionController;
    }
    
    @Override
    public ReferenceFieldSearchFilter getSearchFilter() {
        return filter;
    }
}

