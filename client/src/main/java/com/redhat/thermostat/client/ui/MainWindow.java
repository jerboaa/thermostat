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
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.redhat.thermostat.client.Configuration;
import com.redhat.thermostat.client.AgentConfigurationSource;
import com.redhat.thermostat.client.ApplicationInfo;
import com.redhat.thermostat.client.HostsVMsLoader;
import com.redhat.thermostat.client.MainView;
import com.redhat.thermostat.client.UiFacadeFactory;
import com.redhat.thermostat.client.locale.LocaleResources;
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

        private final DefaultTreeModel treeModel;
        private DefaultMutableTreeNode treeRoot;
        private String filterText;
        private HostsVMsLoader hostsVMsLoader;

        public BackgroundTreeModelWorker(DefaultTreeModel model, DefaultMutableTreeNode root, String filterText, HostsVMsLoader hostsVMsLoader) {
            this.filterText = filterText;
            this.treeModel = model;
            this.treeRoot = root;
            this.hostsVMsLoader = hostsVMsLoader;
        }

        @Override
        protected DefaultMutableTreeNode doInBackground() throws Exception {
            DefaultMutableTreeNode root = new DefaultMutableTreeNode();
            Collection<HostRef> hostsInRemoteModel = hostsVMsLoader.getHosts();
            buildSubTree(root, hostsInRemoteModel, filterText);
            return root;
        }

        private boolean buildSubTree(DefaultMutableTreeNode parent, Collection<? extends Ref> objectsInRemoteModel, String filter) {
            boolean subTreeMatches = false;
            for (Ref inRemoteModel : objectsInRemoteModel) {
                DefaultMutableTreeNode inTreeNode = new DefaultMutableTreeNode(inRemoteModel);

                boolean shouldInsert = false;
                if (filter == null || inRemoteModel.matches(filter)) {
                    shouldInsert = true;
                }

                Collection<? extends Ref> children = getChildren(inRemoteModel);
                boolean subtreeResult = buildSubTree(inTreeNode, children, filter);
                if (subtreeResult) {
                    shouldInsert = true;
                }

                if (shouldInsert) {
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
                        break;
                    }
                }

                if (targetChild == null) {
                    targetChild = new DefaultMutableTreeNode(sourceRef);
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
        }
    }

    private static final long serialVersionUID = 5608972421496808177L;

    private final UiFacadeFactory facadeFactory;

    private JPanel contentArea = null;

    private JTextField searchField = null;
    private JTree agentVmTree = null;

    private final ShutdownClient shutdownAction;

    private ApplicationInfo appInfo;

    private ActionNotifier<Action> actionNotifier = new ActionNotifier<>(this);

    private final DefaultMutableTreeNode publishedRoot =
            new DefaultMutableTreeNode(localize(LocaleResources.MAIN_WINDOW_TREE_ROOT_NAME));
    private final DefaultTreeModel publishedTreeModel = new DefaultTreeModel(publishedRoot);

    public MainWindow(UiFacadeFactory facadeFactory) {
        super();

        appInfo = new ApplicationInfo();
        setTitle(appInfo.getName());

        this.facadeFactory = facadeFactory;
        shutdownAction = new ShutdownClient();

        searchField = new JTextField();
        searchField.setName("hostVMTreeFilter");
        TreeModel model = publishedTreeModel;
        agentVmTree = new JTree(model);
        model.addTreeModelListener(new KeepRootExpandedListener(agentVmTree));
        agentVmTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        agentVmTree.setCellRenderer(new AgentVmTreeCellRenderer());
        ToolTipManager.sharedInstance().registerComponent(agentVmTree);
        contentArea = new JPanel(new BorderLayout());

        setupMenus();
        setupPanels();

        this.setPreferredSize(new Dimension(800, 600));

        agentVmTree.setSelectionPath(new TreePath(((DefaultMutableTreeNode) model.getRoot()).getPath()));

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(shutdownAction);
    }

    private void setupMenus() {
        JMenuBar mainMenuBar = new JMenuBar();

        JMenu fileMenu = new JMenu(localize(LocaleResources.MENU_FILE));
        fileMenu.getPopupMenu().setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        mainMenuBar.add(fileMenu);

        JMenuItem fileConnectMenu = new JMenuItem(localize(LocaleResources.MENU_FILE_CONNECT));
        fileConnectMenu.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                // TODO present a connection dialog
            }
        });
        fileMenu.add(fileConnectMenu);

        fileMenu.add(new Separator());

        JMenuItem fileImportMenu = new JMenuItem(localize(LocaleResources.MENU_FILE_IMPORT));
        fileMenu.add(fileImportMenu);

        JMenuItem fileExportMenu = new JMenuItem(localize(LocaleResources.MENU_FILE_EXPORT));
        fileMenu.add(fileExportMenu);

        fileMenu.add(new Separator());

        JMenuItem fileExitMenu = new JMenuItem(localize(LocaleResources.MENU_FILE_EXIT));
        fileExitMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        fileExitMenu.addActionListener(shutdownAction);
        fileMenu.add(fileExitMenu);

        JMenu editMenu = new JMenu(localize(LocaleResources.MENU_EDIT));
        mainMenuBar.add(editMenu);

        JMenuItem configureAgentMenuItem = new JMenuItem(localize(LocaleResources.MENU_EDIT_CONFIGURE_AGENT));
        configureAgentMenuItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Configuration().showAgentConfiguration();
            }
        });
        editMenu.add(configureAgentMenuItem);

        JMenuItem configureClientMenuItem = new JMenuItem(localize(LocaleResources.MENU_EDIT_CONFIGURE_CLIENT));
        configureClientMenuItem.setName("showClientConfig");
        configureClientMenuItem.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fireViewAction(Action.SHOW_CLIENT_CONFIG);
            }
        });
        editMenu.add(configureClientMenuItem);

        JMenu helpMenu = new JMenu(localize(LocaleResources.MENU_HELP));
        helpMenu.getPopupMenu().setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        mainMenuBar.add(helpMenu);

        JMenuItem helpAboutMenu = new JMenuItem(localize(LocaleResources.MENU_HELP_ABOUT));
        helpAboutMenu.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                AboutDialog aboutDialog = new AboutDialog(appInfo);
                aboutDialog.setModal(true);
                aboutDialog.pack();
                aboutDialog.setVisible(true);
            }
        });
        helpMenu.add(helpAboutMenu);
        setJMenuBar(mainMenuBar);
    }

    private void setupPanels() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        JPanel navigationPanel = new JPanel(new BorderLayout());

        JPanel searchPanel = new JPanel(new BorderLayout());

        navigationPanel.add(searchPanel, BorderLayout.PAGE_START);

        /* the insets are so we can place the actual icon inside the searchField */
        searchField.setMargin(new Insets(0, 0, 0, 30));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent event) {
                changed(event.getDocument());
            }

            @Override
            public void insertUpdate(DocumentEvent event) {
                changed(event.getDocument());
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                changed(event.getDocument());
            }

            private void changed(Document doc) {
                String filter = null;
                try {
                    filter = doc.getText(0, doc.getLength());
                    if (filter.trim().equals("")) {
                        filter = null;
                    }
                } catch (BadLocationException ble) {
                    // ignore
                }
                fireViewAction(MainView.Action.HOST_VM_TREE_FILTER);
            }
        });
        searchPanel.add(searchField);
        // TODO move this icon inside the search field
        JLabel searchIcon = new JLabel(IconResource.SEARCH.getIcon());
        searchIcon.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        searchPanel.add(searchIcon, BorderLayout.LINE_END);

        agentVmTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                if (e.isAddedPath()) {
                    contentArea.removeAll();
                    TreePath path = e.getPath();
                    if (path.getPathCount() == 1) {/* root */
                        contentArea.add(new SummaryPanel(facadeFactory.getSummaryPanel()));
                    } else if (path.getPathCount() == 2) { /* agent */
                        HostRef hostRef = (HostRef) ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
                        HostPanel panel = new HostPanel(facadeFactory.getHostPanel(hostRef));
                        contentArea.add(panel);
                    } else { /* vm */
                        VmRef vmRef = (VmRef) ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
                        VmPanel panel = new VmPanel(facadeFactory.getVmPanel(vmRef));
                        contentArea.add(panel);
                    }
                    contentArea.revalidate();
                }
            }
        });

        JScrollPane treeScrollPane = new JScrollPane(agentVmTree);

        navigationPanel.add(treeScrollPane);

        JPanel detailsPanel = createDetailsPanel();

        navigationPanel.setMinimumSize(new Dimension(200,500));
        detailsPanel.setMinimumSize(new Dimension(500, 500));

        splitPane.add(navigationPanel);
        splitPane.add(detailsPanel);

        add(splitPane);
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

    private static class KeepRootExpandedListener implements TreeModelListener {

        private JTree toModify;

        public KeepRootExpandedListener(JTree treeToModify) {
            toModify = treeToModify;
        }

        @Override
        public void treeStructureChanged(TreeModelEvent e) {
            ensureRootIsExpanded((DefaultTreeModel) e.getSource());
        }

        @Override
        public void treeNodesRemoved(TreeModelEvent e) {
            ensureRootIsExpanded((DefaultTreeModel) e.getSource());
        }

        @Override
        public void treeNodesInserted(TreeModelEvent e) {
            ensureRootIsExpanded((DefaultTreeModel) e.getSource());
        }

        @Override
        public void treeNodesChanged(TreeModelEvent e) {
            ensureRootIsExpanded((DefaultTreeModel) e.getSource());
        }

        private void ensureRootIsExpanded(DefaultTreeModel model) {
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
            toModify.expandPath(new TreePath(root.getPath()));
        }
    }

    private static class AgentVmTreeCellRenderer extends DefaultTreeCellRenderer {
        private static final long serialVersionUID = 4444642511815252481L;

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            setToolTipText(createToolTipText(((DefaultMutableTreeNode) value).getUserObject()));
            return super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
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

    private static class Separator extends JPopupMenu.Separator {

        private static final long serialVersionUID = 3061771592573345826L;

        @Override
        public Dimension getPreferredSize() {
            Dimension result = super.getPreferredSize();
            if (result.height < 1) {
                result.height = 5;
            }
            return result;
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

    @Override
    public void updateTree(String filter, HostsVMsLoader hostsVMsLoader) {
        BackgroundTreeModelWorker worker = new BackgroundTreeModelWorker(publishedTreeModel, publishedRoot, filter, hostsVMsLoader);
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
    public void showMainWindow() {
        pack();
        setVisible(true);
    }

    @Override
    public String getHostVmTreeFilter() {
        return searchField.getText();
    }

}
