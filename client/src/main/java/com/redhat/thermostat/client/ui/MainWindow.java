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

import static com.redhat.thermostat.client.Translate._;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.redhat.thermostat.client.ApplicationInfo;
import com.redhat.thermostat.client.HostRef;
import com.redhat.thermostat.client.MainWindowFacade;
import com.redhat.thermostat.client.UiFacadeFactory;
import com.redhat.thermostat.client.VmRef;

public class MainWindow extends JFrame {

    private static final long serialVersionUID = 5608972421496808177L;

    private final UiFacadeFactory facadeFactory;
    private final MainWindowFacade facade;

    private JPanel contentArea = null;

    private JTextField searchField = null;
    private JTree agentVmTree = null;

    private final ShutdownClient shutdownAction;

    public MainWindow(UiFacadeFactory facadeFactory) {
        super();
        setTitle(_("MAIN_WINDOW_TITLE"));

        this.facadeFactory = facadeFactory;
        this.facade = facadeFactory.getMainWindow();

        shutdownAction = new ShutdownClient(facade, this);

        searchField = new JTextField();
        TreeModel model = facade.getHostVmTree();
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

        this.facade.start();
    }

    private void setupMenus() {
        JMenuBar mainMenuBar = new JMenuBar();

        JMenu fileMenu = new JMenu(_("MENU_FILE"));
        fileMenu.getPopupMenu().setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        mainMenuBar.add(fileMenu);

        JMenuItem fileConnectMenu = new JMenuItem(_("MENU_FILE_CONNECT"));
        fileConnectMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO present a connection dialog
            }
        });
        fileMenu.add(fileConnectMenu);

        fileMenu.add(new Separator());

        JMenuItem fileImportMenu = new JMenuItem(_("MENU_FILE_IMPORT"));
        fileMenu.add(fileImportMenu);

        JMenuItem fileExportMenu = new JMenuItem(_("MENU_FILE_EXPORT"));
        fileMenu.add(fileExportMenu);

        fileMenu.add(new Separator());

        JMenuItem fileExitMenu = new JMenuItem(_("MENU_FILE_EXIT"));
        fileExitMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        fileExitMenu.addActionListener(shutdownAction);
        fileMenu.add(fileExitMenu);

        JMenu helpMenu = new JMenu(_("MENU_HELP"));
        helpMenu.getPopupMenu().setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        mainMenuBar.add(helpMenu);

        JMenuItem helpAboutMenu = new JMenuItem(_("MENU_HELP_ABOUT"));
        helpAboutMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AboutDialog aboutDialog = new AboutDialog(new ApplicationInfo());
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
                facade.setHostVmTreeFilter(filter);
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

    public static class ShutdownClient extends WindowAdapter implements ActionListener {

        private JFrame toDispose;
        private MainWindowFacade facade;

        public ShutdownClient(MainWindowFacade facade, JFrame toDispose) {
            this.facade = facade;
            this.toDispose = toDispose;
        }

        @Override
        public void windowClosing(WindowEvent e) {
            shutdown();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            shutdown();
        }

        private void shutdown() {
            toDispose.dispose();
            facade.stop();
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
                    .appendRaw(_("TREE_HOST_TOOLTIP_HOST_NAME", hostNameHtml))
                    .newLine()
                    .appendRaw(_("TREE_HOST_TOOLTIP_AGENT_ID", agentIdHtml));
                return builder.toHtml();
            } else if (value instanceof VmRef) {
                VmRef vmRef = (VmRef) value;
                String vmNameHtml= new HtmlTextBuilder().bold(vmRef.getName()).toPartialHtml();
                String vmIdHtml = new HtmlTextBuilder().bold(vmRef.getId()).toPartialHtml();
                HtmlTextBuilder builder = new HtmlTextBuilder()
                    .appendRaw(_("TREE_HOST_TOOLTIP_VM_NAME", vmNameHtml))
                    .newLine()
                    .appendRaw(_("TREE_HOST_TOOLTIP_VM_ID", vmIdHtml));
                return builder.toHtml();
            } else {
                return null;
            }
        }
    }

    private static class Separator extends JPopupMenu.Separator {
        @Override
        public Dimension getPreferredSize() {
            Dimension result = super.getPreferredSize();
            if (result.height < 1) {
                result.height = 5;
            }
            return result;
        }
    }

}
