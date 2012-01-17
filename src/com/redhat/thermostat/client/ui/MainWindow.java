package com.redhat.thermostat.client.ui;

import static com.redhat.thermostat.client.Translate._;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

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
import javax.swing.ScrollPaneConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.redhat.thermostat.client.ApplicationInfo;
import com.redhat.thermostat.client.ClientArgs;
import com.redhat.thermostat.client.HostRef;
import com.redhat.thermostat.client.MainWindowFacade;
import com.redhat.thermostat.client.UiFacadeFactory;
import com.redhat.thermostat.client.VmRef;

public class MainWindow extends JFrame {

    private static final long serialVersionUID = 5608972421496808177L;

    private final DefaultMutableTreeNode root = new DefaultMutableTreeNode(_("MAIN_WINDOW_TREE_ROOT_NAME"));
    private final DefaultTreeModel treeModel = new DefaultTreeModel(root);

    private final UiFacadeFactory facadeFactory;
    private final MainWindowFacade facade;

    private JPanel contentArea = null;
    private JTree agentVmTree = null;
    private JTextField searchField = null;

    public MainWindow(UiFacadeFactory facadeFactory) {
        super();
        setTitle(_("MAIN_WINDOW_TITLE"));

        this.facadeFactory = facadeFactory;
        this.facade = facadeFactory.getMainWindow();

        searchField = new JTextField();
        agentVmTree = new AgentVmTree(treeModel);
        contentArea = new VerticalOnlyScrollingPanel();
        contentArea.setLayout(new BorderLayout());

        setupMenus();
        setupPanels();

        agentVmTree.setSelectionPath(new TreePath(root.getPath()));

        buildTree("");

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
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
        fileExitMenu.addActionListener(new ShtudownClient(this));
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
                } catch (BadLocationException ble) {
                    // ignore
                }
                buildTree(filter);
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
                    // Fixes some 'ghosting' caused by the previous components
                    // to stay painted on the JViewPort
                    ((JScrollPane) contentArea.getParent().getParent()).repaint();
                    contentArea.revalidate();
                }
            }

        });

        JScrollPane treeScrollPane = new JScrollPane(agentVmTree);

        navigationPanel.add(treeScrollPane);

        JPanel detailsPanel = createDetailsPanel();

        splitPane.add(navigationPanel);
        splitPane.add(detailsPanel);

        add(splitPane);
    }

    private JPanel createDetailsPanel() {
        JPanel result = new JPanel(new BorderLayout());
        if (ClientArgs.isDebugLayout()) {
            contentArea.setBorder(BorderFactory.createLineBorder(Color.GREEN));
        }
        JScrollPane contentScrollPane = new JScrollPane(contentArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        result.add(contentScrollPane, BorderLayout.CENTER);
        if (ClientArgs.isDebugLayout()) {
            result.setBorder(BorderFactory.createLineBorder(Color.PINK));
        }
        return result;
    }

    private void buildTree(String filter) {
        root.removeAllChildren();
        treeModel.setRoot(null);
        // paths to expand. only expand paths when a vm matches (to ensure it is
        // visible)

        List<TreeNode[]> pathsToExpand = new ArrayList<TreeNode[]>();
        if (filter == null || filter.trim().equals("")) {
            DefaultMutableTreeNode agentNode;
            HostRef[] agentRefs = facade.getHosts();
            for (HostRef hostRef : agentRefs) {
                agentNode = new DefaultMutableTreeNode(hostRef);
                root.add(agentNode);
                VmRef[] vmRefs = facade.getVms(hostRef);
                for (VmRef vmRef : vmRefs) {
                    agentNode.add(new DefaultMutableTreeNode(vmRef));
                }
            }
            treeModel.setRoot(root);
        } else {
            DefaultMutableTreeNode agentNode;
            for (HostRef hostRef : facade.getHosts()) {
                if (hostRef.getName().contains(filter) || hostRef.getAgentId().contains(filter)) {
                    agentNode = new DefaultMutableTreeNode(hostRef);
                    root.add(agentNode);
                    VmRef[] vmRefs = facade.getVms(hostRef);
                    for (VmRef vmRef : vmRefs) {
                        agentNode.add(new DefaultMutableTreeNode(vmRef));
                    }
                } else {
                    agentNode = null;
                    for (VmRef vmRef : facade.getVms(hostRef)) {
                        if (vmRef.getName().contains(filter) || vmRef.getId().contains(filter)) {
                            if (agentNode == null) {
                                agentNode = new DefaultMutableTreeNode(hostRef);
                                root.add(agentNode);
                            }
                            DefaultMutableTreeNode vmNode = new DefaultMutableTreeNode(vmRef);
                            agentNode.add(vmNode);
                            pathsToExpand.add(vmNode.getPath());
                        }
                    }
                }
            }
            if (root.getChildCount() > 0) {
                treeModel.setRoot(root);
            }

        }
        for (TreeNode[] path : pathsToExpand) {
            agentVmTree.expandPath(new TreePath(path).getParentPath());
        }
        agentVmTree.expandRow(0);
    }

    public static class ShtudownClient implements ActionListener {

        private JFrame toDispose;

        public ShtudownClient(JFrame toDispose) {
            this.toDispose = toDispose;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            toDispose.dispose();
        }
    }

    private static class AgentVmTree extends JTree {
        private static final long serialVersionUID = 8894141735861100579L;

        public AgentVmTree(TreeModel model) {
            super(model);
            getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
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
