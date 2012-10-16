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

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

import com.redhat.thermostat.client.heap.HeapObjectUI;
import com.redhat.thermostat.client.heap.LocaleResources;
import com.redhat.thermostat.client.heap.ObjectRootsView;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.swing.EdtHelper;

@SuppressWarnings("serial")
public class ObjectRootsFrame extends JFrame implements ObjectRootsView {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    /** For TESTING ONLY! */
    static final String TREE_NAME = "roots-tree";

    static final String DETAILS_NAME = "object-details";

    private final ActionNotifier<Action> notifier = new ActionNotifier<>(this);

    private final LazyMutableTreeNode ROOT = new LazyMutableTreeNode();
    private final DefaultTreeModel dataModel;
    private final JTree pathToRootTree;

    private final JTextPane objectDetails;

    public ObjectRootsFrame() {
        setTitle(translator.localize(LocaleResources.OBJECT_ROOTS_VIEW_TITLE));

        dataModel = new DefaultTreeModel(ROOT);
        pathToRootTree = new JTree(dataModel);
        pathToRootTree.setName(TREE_NAME);

        JLabel lblNewLabel = new JLabel(translator.localize(LocaleResources.OBJECT_ROOTS_VIEW_TITLE));

        JScrollPane scrollPane = new JScrollPane(pathToRootTree);

        objectDetails = new JTextPane();
        objectDetails.setName(DETAILS_NAME);

        GroupLayout groupLayout = new GroupLayout(getContentPane());
        groupLayout.setHorizontalGroup(
            groupLayout.createParallelGroup(Alignment.TRAILING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                        .addComponent(scrollPane, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 416, Short.MAX_VALUE)
                        .addComponent(objectDetails, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 416, Short.MAX_VALUE)
                        .addComponent(lblNewLabel, Alignment.LEADING))
                    .addContainerGap())
        );
        groupLayout.setVerticalGroup(
            groupLayout.createParallelGroup(Alignment.LEADING)
                .addGroup(groupLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(lblNewLabel)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE)
                    .addPreferredGap(ComponentPlacement.RELATED)
                    .addComponent(objectDetails, GroupLayout.PREFERRED_SIZE, 93, GroupLayout.PREFERRED_SIZE)
                    .addContainerGap())
        );
        getContentPane().setLayout(groupLayout);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                notifier.fireAction(Action.VISIBLE);
            }

            @Override
            public void windowClosing(WindowEvent e) {
                notifier.fireAction(Action.HIDDEN);
            }
        });

        pathToRootTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                HeapObjectUI obj = (HeapObjectUI) ((LazyMutableTreeNode)e.getPath().getLastPathComponent()).getUserObject();
                notifier.fireAction(Action.OBJECT_SELECTED, obj);
            }
        });


    }

    @Override
    public void addActionListener(ActionListener<Action> listener) {
        notifier.addActionListener(listener);
    }

    @Override
    public void removeActionListener(ActionListener<Action> listener) {
        notifier.removeActionListener(listener);
    }

    @Override
    public void showView() {
        Callable<Boolean> hideViewRunnable = new Callable<Boolean>() {
            @Override
            public Boolean call() {
                pack();
                setVisible(true);
                return new Boolean(true);
            }
        };
        try {
            new EdtHelper().callAndWait(hideViewRunnable);
        } catch (InvocationTargetException | InterruptedException e) {
            InternalError error = new InternalError();
            error.initCause(e);
            throw error;
        }
    }

    @Override
    public void hideView() {
        Callable<Boolean> hideViewRunnable = new Callable<Boolean>() {
            @Override
            public Boolean call() {
                setVisible(false);
                dispose();
                return new Boolean(true);
            }
        };
        try {
            new EdtHelper().callAndWait(hideViewRunnable);
        } catch (InvocationTargetException | InterruptedException e) {
            InternalError error = new InternalError();
            error.initCause(e);
            throw error;
        }
    }

    @Override
    public void setPathToRoot(List<HeapObjectUI> pathToRoot) {
        final List<HeapObjectUI> path = new ArrayList<>(pathToRoot);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                clearTree();

                LazyMutableTreeNode node = ROOT;
                node.setUserObject(path.get(0));
                TreePath treePath = new TreePath(node);
                LazyMutableTreeNode parent = ROOT;
                for (int i = 1; i < path.size(); i++) {
                    HeapObjectUI obj = path.get(i);
                    node = new LazyMutableTreeNode(obj);
                    dataModel.insertNodeInto(node, parent, node.getChildCount());
                    parent = node;
                    treePath = treePath.pathByAddingChild(node);
                }

                pathToRootTree.expandPath(treePath);
                pathToRootTree.setSelectionPath(treePath);
            }
        });
    }

    private void clearTree() {
        // clear children from model
        int childrenCount = ROOT.getChildCount();
        for (int i = 0; i < childrenCount; i++) {
            MutableTreeNode child = (MutableTreeNode) ROOT.getChildAt(0);
            dataModel.removeNodeFromParent(child);
        }
    }

    @Override
    public void setObjectDetails(final String information) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                objectDetails.setText(information);
            }
        });
    }

}
