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

package com.redhat.thermostat.client.heap.swing;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.GroupLayout;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JLabel;

import com.redhat.thermostat.client.heap.HeapDumpDetailsView.HeapObjectUI;
import com.redhat.thermostat.client.heap.HeapDumpDetailsView.ObjectReferenceCallback;
import com.redhat.thermostat.client.heap.LocaleResources;
import com.redhat.thermostat.client.heap.Translate;
import com.redhat.thermostat.client.ui.EdtHelper;
import com.redhat.thermostat.client.ui.SearchFieldSwingView;
import com.redhat.thermostat.client.ui.SearchFieldView;
import com.sun.tools.hat.internal.model.JavaHeapObject;

import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import java.awt.BorderLayout;
import javax.swing.JSplitPane;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * A Panel that displays JavaHeapObjects and referrers and references.
 */
@SuppressWarnings("serial")
class ObjectDetailsPanel extends JPanel {

    private final SearchFieldSwingView searchField;

    private final LazyMutableTreeNode ROOT = new LazyMutableTreeNode(null);
    /** all the nodes in this model must be {@link LazyMutableTreeNode}s */
    private final DefaultTreeModel model = new DefaultTreeModel(ROOT);
    private final JTree objectTree = new JTree(model);

    private final JTextPane objectDetailsPane;

    private final ButtonGroup refGroup = new ButtonGroup();
    private final JToggleButton toggleReferrersButton;
    private final JToggleButton toggleReferencesButton;

    private final List<ObjectReferenceCallback> callbacks = new CopyOnWriteArrayList<>();

    public ObjectDetailsPanel() {

        JLabel searchLabel = new JLabel(Translate.localize(LocaleResources.HEAP_DUMP_OBJECT_BROWSE_SEARCH_LABEL));

        searchField = new SearchFieldSwingView();

        JSplitPane splitPane = new JSplitPane();
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(0.8 /* 80% */);

        toggleReferrersButton = new JToggleButton(Translate.localize(LocaleResources.HEAP_DUMP_OBJECT_BROWSE_REFERRERS));
        refGroup.add(toggleReferrersButton);

        toggleReferencesButton = new JToggleButton(Translate.localize(LocaleResources.HEAP_DUMP_OBJECT_BROWSE_REFERENCES));
        refGroup.add(toggleReferencesButton);

        toggleReferrersButton.setSelected(true);

        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                        .addGroup(groupLayout.createSequentialGroup()
                            .addComponent(searchLabel)
                            .addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(toggleReferencesButton)
                            .addPreferredGap(ComponentPlacement.RELATED)
                            .addComponent(toggleReferrersButton))
                        .addGroup(groupLayout.createSequentialGroup()
                            .addGap(12)
                            .addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
                                .addComponent(splitPane, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 447, Short.MAX_VALUE)
                                .addComponent(searchField, GroupLayout.DEFAULT_SIZE, 447, Short.MAX_VALUE))))
                    .addContainerGap())
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
                        .addComponent(searchLabel)
                        .addComponent(toggleReferrersButton)
                        .addComponent(toggleReferencesButton))
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(searchField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(splitPane, GroupLayout.DEFAULT_SIZE, 314, Short.MAX_VALUE)
                    .addContainerGap())
        );

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(objectTree);

        splitPane.setLeftComponent(scrollPane);

        JPanel panel = new JPanel();
        splitPane.setRightComponent(panel);

        panel.setLayout(new BorderLayout(0, 0));

        objectDetailsPane = new JTextPane();
        objectDetailsPane.setEditable(false);
        panel.add(objectDetailsPane);
        setLayout(groupLayout);

        initializeTree();
        clearTree();
    }

    private void initializeTree() {
        objectTree.setRootVisible(false);
        objectTree.setShowsRootHandles(true);
        objectTree.setEditable(false);
        if (objectTree.getCellRenderer() instanceof DefaultTreeCellRenderer) {
            DefaultTreeCellRenderer cellRenderer = (DefaultTreeCellRenderer) objectTree.getCellRenderer();
            cellRenderer.setClosedIcon(null);
            cellRenderer.setOpenIcon(null);
            cellRenderer.setLeafIcon(null);
        }

        objectTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        objectTree.expandPath(new TreePath(ROOT.getPath()));

        objectTree.addTreeWillExpandListener(new TreeWillExpandListener() {

            @Override
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                if (new TreePath(ROOT.getPath()).equals(event.getPath())) {
                    return;
                }

                lazyLoadChildren(event.getPath());
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
                if (new TreePath(ROOT.getPath()).equals(event.getPath())) {
                    throw new ExpandVetoException(event, "root cant be collapsed");
                }
            }
        });
    }

    private void clearTree() {
        // clear children from model
        int childrenCount = ROOT.getChildCount();
        for (int i = 0; i < childrenCount; i++) {
            MutableTreeNode child = (MutableTreeNode) ROOT.getChildAt(0);
            model.removeNodeFromParent(child);
        }
    }

    private void lazyLoadChildren(TreePath path) {
        LazyMutableTreeNode node = (LazyMutableTreeNode) path.getLastPathComponent();
        if (node.getChildCount() > 0) {
            // already processed
            return;
        }

        if (toggleReferencesButton.isSelected()) {
            addReferences(node);
        } else if (toggleReferrersButton.isSelected()) {
            addReferrers(node);
        }
    }

    private void addReferrers(LazyMutableTreeNode node) {
        HeapObjectUI data = (HeapObjectUI) node.getUserObject();

        List<HeapObjectUI> referrers = new ArrayList<>();
        for (ObjectReferenceCallback callback : callbacks) {
            referrers.addAll(callback.getReferrers(data));
        }

        for (HeapObjectUI obj: referrers) {
            model.insertNodeInto(new LazyMutableTreeNode(obj), node, node.getChildCount());
        }
    }

    private void addReferences(LazyMutableTreeNode node) {
        HeapObjectUI data = (HeapObjectUI) node.getUserObject();

        List<HeapObjectUI> referrers = new ArrayList<>();
        for (ObjectReferenceCallback callback : callbacks) {
            referrers.addAll(callback.getReferences(data));
        }

        for (HeapObjectUI obj: referrers) {
            model.insertNodeInto(new LazyMutableTreeNode(obj), node, node.getChildCount());
        }
    }

    public SearchFieldView getSearchField() {
        return searchField;
    }

    public JToggleButton[] getTreeModeButtons() {
        return new JToggleButton[] { toggleReferencesButton, toggleReferrersButton };
    }

    public JTree getObjectTree() {
        return objectTree;
    }

    public void addObjectReferenceCallback(ObjectReferenceCallback callback) {
        callbacks.add(callback);
    }

    public void removeObjectReferenceCallback(ObjectReferenceCallback callback) {
        callbacks.remove(callback);
    }

    public void setMatchingObjects(final Collection<HeapObjectUI> objects) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // clear children
                clearTree();

                // add new children
                for (HeapObjectUI object: objects) {
                    MutableTreeNode node = new LazyMutableTreeNode(object);
                    model.insertNodeInto(node, ROOT, ROOT.getChildCount());
                }
                objectTree.expandPath(new TreePath(ROOT.getPath()));
            }
        });
    }

    public void setObjectDetails(final JavaHeapObject obj) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // TODO use some other gui control for this rather than a plain text box
                String text = Translate.localize(LocaleResources.COMMAND_OBJECT_INFO_OBJECT_ID) + obj.getIdString() + "\n" +
                              Translate.localize(LocaleResources.COMMAND_OBJECT_INFO_TYPE) + obj.getClazz().getName() + "\n" +
                              Translate.localize(LocaleResources.COMMAND_OBJECT_INFO_SIZE) + String.valueOf(obj.getSize()) + " bytes" + "\n" +
                              Translate.localize(LocaleResources.COMMAND_OBJECT_INFO_HEAP_ALLOCATED) + String.valueOf(obj.isHeapAllocated()) + "\n";
                objectDetailsPane.setText(text);
            }
        });
    }

    /**
     * @return null if no selected object
     */
    public HeapObjectUI getSelectedMatchingObject() {
        try {
            return new EdtHelper().callAndWait(new Callable<HeapObjectUI>() {
                @Override
                public HeapObjectUI call() throws Exception {
                    LazyMutableTreeNode node = (LazyMutableTreeNode) objectTree.getSelectionPath().getLastPathComponent();
                    return (HeapObjectUI) node.getUserObject();
                }
            });
        } catch (InvocationTargetException | InterruptedException e) {
            return null;
        }
    }

    private static class LazyMutableTreeNode extends javax.swing.tree.DefaultMutableTreeNode {

        public LazyMutableTreeNode(HeapObjectUI heapObjectUI) {
            super(heapObjectUI);
        }

        @Override
        public boolean isLeaf() {
            return !getAllowsChildren();
        }
    }
}
