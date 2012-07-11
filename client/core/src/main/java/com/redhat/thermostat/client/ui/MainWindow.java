/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.client.ui;

import static com.redhat.thermostat.client.locale.Translate.localize;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.redhat.thermostat.client.internal.HostsVMsLoader;
import com.redhat.thermostat.client.internal.MainView;
import com.redhat.thermostat.client.locale.LocaleResources;
import com.redhat.thermostat.client.osgi.service.Filter;
import com.redhat.thermostat.client.osgi.service.MenuAction;
import com.redhat.thermostat.client.osgi.service.ReferenceDecorator;
import com.redhat.thermostat.client.osgi.service.VMContextAction;
import com.redhat.thermostat.client.ui.SearchFieldView.SearchAction;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.Ref;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.utils.StringUtils;

public class MainWindow extends JFrame implements MainView {

    /**
     * Updates a TreeModel in the background in an Swing EDT-safe manner.
     */
    private static class BackgroundTreeModelWorker extends SwingWorker<DefaultMutableTreeNode, Void> {

        private JTree tree;

        private final DefaultTreeModel treeModel;
        private DefaultMutableTreeNode treeRoot;
        
        private List<Filter> filters;
        private List<ReferenceDecorator> decorators;
        
        private HostsVMsLoader hostsVMsLoader;

        public BackgroundTreeModelWorker(DefaultTreeModel model, DefaultMutableTreeNode root,
                                         List<Filter> filters, List<ReferenceDecorator> decorators,
                                         HostsVMsLoader hostsVMsLoader, JTree tree)
        {
            this.filters = filters;
            this.treeModel = model;
            this.treeRoot = root;
            this.hostsVMsLoader = hostsVMsLoader;
            this.decorators = decorators;
            this.tree = tree;
        }

        @Override
        protected DefaultMutableTreeNode doInBackground() throws Exception {
            DefaultMutableTreeNode root = new DefaultMutableTreeNode();
            
            Collection<HostRef> hostsInRemoteModel = hostsVMsLoader.getHosts();
            buildSubTree(root, hostsInRemoteModel);
            return root;
        }

        private boolean buildSubTree(DefaultMutableTreeNode parent, Collection<? extends Ref> objectsInRemoteModel) {
            boolean subTreeMatches = false;
            for (Ref inRemoteModel : objectsInRemoteModel) {
                DecoratedDefaultMutableTreeNode inTreeNode =
                        new DecoratedDefaultMutableTreeNode(inRemoteModel);

                boolean shouldInsert = false;
                if (filters == null) {
                    shouldInsert = true;
                } else {
                    shouldInsert = true;
                    for (Filter filter : filters) {
                        if (!filter.matches(inRemoteModel)) {
                            shouldInsert = false;
                            break;
                        }
                    }
                }
                
                Collection<? extends Ref> children = getChildren(inRemoteModel);
                boolean subtreeResult = buildSubTree(inTreeNode, children);
                if (subtreeResult) {
                    shouldInsert = true;
                }

                if (shouldInsert) {
                    for (ReferenceDecorator decorator : decorators) {
                        Filter filter = decorator.getFilter();
                        if (filter != null && filter.matches(inRemoteModel)) {
                            inTreeNode.addDecorator(decorator);
                        }
                    }
                    
                    parent.add(inTreeNode);
                    subTreeMatches = true;
                }
            }
            
            return subTreeMatches;
        }

        @Override
        protected void done() {
            DefaultMutableTreeNode sourceRoot;
            try {
                sourceRoot = get();
                syncTree(sourceRoot, treeModel, treeRoot);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        private Collection<? extends Ref> getChildren(Ref parent) {
            if (parent == null) {
                return hostsVMsLoader.getHosts();
            } else if (parent instanceof HostRef) {
                HostRef host = (HostRef) parent;
                return hostsVMsLoader.getVMs(host);
            }
            return Collections.emptyList();
        }

        private void syncTree(DefaultMutableTreeNode sourceRoot, DefaultTreeModel targetModel, DefaultMutableTreeNode targetNode) {
            
            @SuppressWarnings("unchecked") // We know what we put into these trees.
            List<DefaultMutableTreeNode> sourceChildren = Collections.list(sourceRoot.children());

            @SuppressWarnings("unchecked")
            List<DefaultMutableTreeNode> targetChildren = Collections.list(targetNode.children());
            for (DefaultMutableTreeNode sourceChild : sourceChildren) {
                Ref sourceRef = (Ref) sourceChild.getUserObject();
                DefaultMutableTreeNode targetChild = null;
                for (DefaultMutableTreeNode aChild : targetChildren) {
                    Ref targetRef = (Ref) aChild.getUserObject();
                    if (targetRef.equals(sourceRef)) {
                        targetChild = aChild;
                        if (sourceChild instanceof DecoratedDefaultMutableTreeNode) {
                            DecoratedDefaultMutableTreeNode source = (DecoratedDefaultMutableTreeNode) sourceChild;
                            ((DecoratedDefaultMutableTreeNode) targetChild).setDecorators(source.getDecorators());
                        }
                        break;
                    }
                }

                if (targetChild == null) {
                    targetChild = new DecoratedDefaultMutableTreeNode(sourceRef);
                    if (sourceChild instanceof DecoratedDefaultMutableTreeNode) {
                        DecoratedDefaultMutableTreeNode source = (DecoratedDefaultMutableTreeNode) sourceChild;
                        ((DecoratedDefaultMutableTreeNode) targetChild).setDecorators(source.getDecorators());
                    }
                    targetModel.insertNodeInto(targetChild, targetNode, targetNode.getChildCount());
                }

                syncTree(sourceChild, targetModel, targetChild);
            }

            for (DefaultMutableTreeNode targetChild : targetChildren) {
                Ref targetRef = (Ref) targetChild.getUserObject();
                boolean matchFound = false;
                for (DefaultMutableTreeNode sourceChild : sourceChildren) {
                    Ref sourceRef = (Ref) sourceChild.getUserObject();
                    if (targetRef.equals(sourceRef)) {
                        matchFound = true;
                        break;
                    }
                }

                if (!matchFound) {
                    targetModel.removeNodeFromParent(targetChild);
                }
            }
            ensureRootIsExpanded(targetModel);
        }

        private void ensureRootIsExpanded(final DefaultTreeModel model) {
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
            tree.expandPath(new TreePath(root.getPath()));
        }

    }

    private static final long serialVersionUID = 5608972421496808177L;

    private final JMenuBar mainMenuBar = new JMenuBar();
    private final MenuHelper mainMenuHelper = new MenuHelper(mainMenuBar);
    private JPanel contentArea = null;

    private SearchFieldSwingView searchField = new SearchFieldSwingView();
    private JTree agentVmTree = null;

    private final ShutdownClient shutdownAction;

    private ActionNotifier<Action> actionNotifier = new ActionNotifier<>(this);

    private JPopupMenu vmContextMenu;
    
    private final DefaultMutableTreeNode publishedRoot =
            new DefaultMutableTreeNode(localize(LocaleResources.MAIN_WINDOW_TREE_ROOT_NAME));
    private final DefaultTreeModel publishedTreeModel = new DefaultTreeModel(publishedRoot);

    public MainWindow() {
        super();

        shutdownAction = new ShutdownClient();

        searchField.addActionListener(new ActionListener<SearchAction>() {
            @Override
            public void actionPerformed(ActionEvent<SearchAction> actionEvent) {
                switch (actionEvent.getActionId()) {
                case TEXT_CHANGED:
                    fireViewAction(Action.HOST_VM_TREE_FILTER);
                    break;
                }
            }
        });
        agentVmTree = new JTree(publishedTreeModel);
        agentVmTree.setName("agentVmTree");
        agentVmTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        agentVmTree.setCellRenderer(new AgentVmTreeCellRenderer());
        ToolTipManager.sharedInstance().registerComponent(agentVmTree);
        contentArea = new JPanel(new BorderLayout());

        setupMenus();
        setupPanels();

        this.setPreferredSize(new Dimension(800, 600));

        agentVmTree.setSelectionPath(new TreePath(((DefaultMutableTreeNode) publishedTreeModel.getRoot()).getPath()));
        
        //agentVmTree.setLargeModel(true);
        agentVmTree.setRowHeight(25);
        
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(shutdownAction);

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

    private void setupMenus() {

        JMenu fileMenu = new JMenu(localize(LocaleResources.MENU_FILE));
        fileMenu.getPopupMenu().setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        mainMenuBar.add(fileMenu);

        JMenuItem fileExitMenu = new JMenuItem(localize(LocaleResources.MENU_FILE_EXIT));
        fileExitMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        fileExitMenu.addActionListener(shutdownAction);
        fileMenu.add(fileExitMenu);

        JMenu editMenu = new JMenu(localize(LocaleResources.MENU_EDIT));
        mainMenuBar.add(editMenu);

        JMenuItem configureAgentMenuItem = new JMenuItem(localize(LocaleResources.MENU_EDIT_CONFIGURE_AGENT));
        configureAgentMenuItem.setName("showAgentConfig");
        configureAgentMenuItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                fireViewAction(Action.SHOW_AGENT_CONFIG);
            }
        });
        editMenu.add(configureAgentMenuItem);

        JMenuItem configureClientMenuItem = new JMenuItem(localize(LocaleResources.MENU_EDIT_CONFIGURE_CLIENT));
        configureClientMenuItem.setName("showClientConfig");
        configureClientMenuItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                fireViewAction(Action.SHOW_CLIENT_CONFIG);
            }
        });
        editMenu.add(configureClientMenuItem);

        editMenu.addSeparator();
        JMenuItem historyModeMenuItem = new JCheckBoxMenuItem(localize(LocaleResources.MENU_EDIT_ENABLE_HISTORY_MODE));
        historyModeMenuItem.setName("historyModeSwitch");
        historyModeMenuItem.setSelected(false);
        historyModeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                fireViewAction(Action.SWITCH_HISTORY_MODE);
            }
        });

        editMenu.add(historyModeMenuItem);
        JMenu helpMenu = new JMenu(localize(LocaleResources.MENU_HELP));
        helpMenu.getPopupMenu().setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        mainMenuBar.add(helpMenu);

        JMenuItem helpAboutMenu = new JMenuItem(localize(LocaleResources.MENU_HELP_ABOUT));
        helpAboutMenu.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                fireViewAction(Action.SHOW_ABOUT_DIALOG);
            }
        });
        helpMenu.add(helpAboutMenu);
        setJMenuBar(mainMenuBar);
    }

    private void setupPanels() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        JPanel navigationPanel = new JPanel(new BorderLayout());

        navigationPanel.add(searchField, BorderLayout.PAGE_START);

        agentVmTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                if (e.isAddedPath()) {
                    fireViewAction(Action.HOST_VM_SELECTION_CHANGED);
                }
            }
        });
        registerContextActionListener(agentVmTree);
        
        JScrollPane treeScrollPane = new JScrollPane(agentVmTree);

        navigationPanel.add(treeScrollPane);

        JPanel detailsPanel = createDetailsPanel();

        navigationPanel.setMinimumSize(new Dimension(200,500));
        detailsPanel.setMinimumSize(new Dimension(500, 500));

        splitPane.add(navigationPanel);
        splitPane.add(detailsPanel);

        add(splitPane);
    }

    private void registerContextActionListener(JTree agentVmTree2) {
        vmContextMenu = new JPopupMenu();
        agentVmTree2.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    Ref ref = getSelectedHostOrVm();
                    if (ref instanceof VmRef) {
                        fireViewAction(Action.SHOW_VM_CONTEXT_MENU, e);
                    }
                }
            }
        });
    }

    @Override
    public void showVMContextActions(final List<VMContextAction> actions, final MouseEvent e) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                vmContextMenu.removeAll();

                for (final VMContextAction action: actions) {
                    JMenuItem contextAction = new JMenuItem();
                    contextAction.setText(action.getName());
                    contextAction.setToolTipText(action.getDescription());

                    contextAction.addActionListener(new java.awt.event.ActionListener() {
                        @Override
                        public void actionPerformed(java.awt.event.ActionEvent e) {
                            fireViewAction(Action.VM_CONTEXT_ACTION, action);
                        }
                    });
                    vmContextMenu.add(contextAction);
                }

                vmContextMenu.show((Component)e.getSource(), e.getX(), e.getY());
            }

        });
    }
    
    private JPanel createDetailsPanel() {
        JPanel result = new JPanel(new BorderLayout());
        result.add(contentArea, BorderLayout.CENTER);
        return result;
    }

    public class ShutdownClient extends WindowAdapter implements java.awt.event.ActionListener {

        @Override
        public void windowClosing(WindowEvent e) {
            shutdown();
        }

        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            shutdown();
        }

        private void shutdown() {
            dispose();
            fireViewAction(Action.SHUTDOWN);
        }
    }

    private static class AgentVmTreeCellRenderer extends DefaultTreeCellRenderer {
        private static final long serialVersionUID = 4444642511815252481L;

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            
            Object node = ((DefaultMutableTreeNode) value).getUserObject();
            setToolTipText(createToolTipText(node));
            
            Component component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof DecoratedDefaultMutableTreeNode) {
                DecoratedDefaultMutableTreeNode treeNode = (DecoratedDefaultMutableTreeNode) value;
                setAnnotation(treeNode, node, component);
            }

            return component;
        }
        
        // TODO: we can cache more, for example the full icon, not just the decoration
        private Map<Decorator, ImageIcon> decoratorsCache = new HashMap<>();
        private void setAnnotation(DecoratedDefaultMutableTreeNode treeNode, Object value, Component component) {

            List<ReferenceDecorator> decorators = treeNode.getDecorators();
            for (ReferenceDecorator decorator : decorators) {
                Decorator dec = decorator.getDecorator();
                String newText = dec.getLabel(getText());
                setText(newText);
                setLabelFor(component);
                
                ImageIcon icon = decoratorsCache.get(dec);
                if (icon == null) {
                    //System.err.println("cache miss: " + dec);
                    IconDescriptor iconDescriptor = dec.getIconDescriptor();
                    if (iconDescriptor != null) {
                        ByteBuffer data = iconDescriptor.getData();
                        icon = new ImageIcon(data.array());
                        decoratorsCache.put(dec, icon);
                    }
                }
                
                if (icon == null) {
                    return;
                }
                
                Icon currentIcon = getIcon();
                switch (dec.getQuadrant()) {
                case BOTTOM_LEFT:
                    int y = currentIcon.getIconHeight() - icon.getIconHeight();
                    paintCustomIcon(currentIcon, icon, y);
                    break;
                    
                case TOP_LEFT:
                    paintCustomIcon(currentIcon, icon, 0);
                    break;
                    
                case MAIN:
                default:
                    setIcon(icon);
                    break;
                }
            }
        }
        
        private void paintCustomIcon(Icon currentIcon, ImageIcon icon, int y) {
            BufferedImage image = new BufferedImage(currentIcon.getIconWidth(),
                                                    currentIcon.getIconHeight(),
                                                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = (Graphics2D) image.getGraphics();
            
            currentIcon.paintIcon(null, graphics, 0, 0);
            graphics.drawImage(icon.getImage(), 0, y, null);
            
            setIcon(new ImageIcon(image));
        }
        
        private String createToolTipText(Object value) {
            if (value instanceof HostRef) {
                HostRef hostRef = (HostRef) value;
                String hostNameHtml = new HtmlTextBuilder().bold(hostRef.getHostName()).toPartialHtml();
                String agentIdHtml = new HtmlTextBuilder().bold(hostRef.getAgentId()).toPartialHtml();
                HtmlTextBuilder builder = new HtmlTextBuilder()
                    .appendRaw(localize(LocaleResources.TREE_HOST_TOOLTIP_HOST_NAME, hostNameHtml))
                    .newLine()
                    .appendRaw(localize(LocaleResources.TREE_HOST_TOOLTIP_AGENT_ID, agentIdHtml));
                return builder.toHtml();
            } else if (value instanceof VmRef) {
                VmRef vmRef = (VmRef) value;
                String vmNameHtml= new HtmlTextBuilder().bold(vmRef.getName()).toPartialHtml();
                String vmIdHtml = new HtmlTextBuilder().bold(vmRef.getIdString()).toPartialHtml();
                HtmlTextBuilder builder = new HtmlTextBuilder()
                    .appendRaw(localize(LocaleResources.TREE_HOST_TOOLTIP_VM_NAME, vmNameHtml))
                    .newLine()
                    .appendRaw(localize(LocaleResources.TREE_HOST_TOOLTIP_VM_ID, vmIdHtml));
                return builder.toHtml();
            } else {
                return null;
            }
        }
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
    
    private void fireViewAction(Action action, Object payload) {
        actionNotifier.fireAction(action, payload);
    }
    
    @Override
    public void updateTree(List<Filter> filters, List<ReferenceDecorator> decorators, HostsVMsLoader hostsVMsLoader) {
        BackgroundTreeModelWorker worker =
                new BackgroundTreeModelWorker(publishedTreeModel, publishedRoot,
                                              filters, decorators, hostsVMsLoader, agentVmTree);
        worker.execute();
    }

    @SuppressWarnings("unused") // Used for debugging but not in production code.
    private static void printTree(PrintStream out, TreeNode node, int depth) {
        out.println(StringUtils.repeat("  ", depth) + node.toString());
        @SuppressWarnings("unchecked")
        List<TreeNode> children = Collections.list(node.children());
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
    public void setSubView(final Component view) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                contentArea.removeAll();
                Component toAdd = view;
                contentArea.add(toAdd);
                contentArea.revalidate();
            }
        });
    }

    @Override
    public void addMenu(MenuAction action) {
        mainMenuHelper.addMenuAction(action);
    }

    @Override
    public void removeMenu(MenuAction action) {
        mainMenuHelper.removeMenuAction(action);
    }

    /**
     * Returns null to indicate no Ref is selected
     */
    @Override
    public Ref getSelectedHostOrVm() {
        TreePath path = agentVmTree.getSelectionPath();
        if (path == null || path.getPathCount() == 1) {
            return null;
        }
        return (Ref) ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
    }

    @Override
    public String getHostVmTreeFilterText() {
        return searchField.getSearchText();
    }
}
