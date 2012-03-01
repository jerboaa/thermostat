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

package com.redhat.thermostat.client;

import static com.redhat.thermostat.client.Translate.localize;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.common.utils.StringUtils;

public class MainWindowFacadeImpl implements MainWindowFacade {

    private static final Logger logger = LoggingUtils.getLogger(MainWindowFacadeImpl.class);

    private final DBCollection agentConfigCollection;
    private final DBCollection hostInfoCollection;
    private final DBCollection vmInfoCollection;

    private final DefaultMutableTreeNode publishedRoot = new DefaultMutableTreeNode(localize("MAIN_WINDOW_TREE_ROOT_NAME"));
    private final DefaultTreeModel publishedTreeModel = new DefaultTreeModel(publishedRoot);

    private String filterText;

    private final Timer backgroundUpdater = new Timer();

    public MainWindowFacadeImpl(DB db) {
        this.agentConfigCollection = db.getCollection("agent-config");
        this.hostInfoCollection = db.getCollection("host-info");
        this.vmInfoCollection = db.getCollection("vm-info");
    }

    @Override
    public void start() {
        backgroundUpdater.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                doUpdateTreeAsync();
            }
        }, 0, TimeUnit.SECONDS.toMillis(10));
    }

    @Override
    public void stop() {
        backgroundUpdater.cancel();
    }

    @Override
    public HostRef[] getHosts() {
        List<HostRef> hostRefs = new ArrayList<HostRef>();

        DBCursor cursor = agentConfigCollection.find();
        while (cursor.hasNext()) {
            DBObject doc = cursor.next();
            String id = (String) doc.get("agent-id");
            if (id != null) {
                DBObject hostInfo = hostInfoCollection.findOne(new BasicDBObject("agent-id", id));
                String hostName = (String) hostInfo.get("hostname");
                HostRef agent = new HostRef(id, hostName);
                hostRefs.add(agent);
            }
        }
        logger.log(Level.FINER, "found " + hostRefs.size() + " connected agents");
        return hostRefs.toArray(new HostRef[0]);
    }

    @Override
    public VmRef[] getVms(HostRef hostRef) {
        List<VmRef> vmRefs = new ArrayList<VmRef>();
        DBCursor cursor = vmInfoCollection.find(new BasicDBObject("agent-id", hostRef.getAgentId()));
        while (cursor.hasNext()) {
            DBObject vmObject = cursor.next();
            Integer id = (Integer) vmObject.get("vm-id");
            // TODO can we do better than the main class?
            String mainClass = (String) vmObject.get("main-class");
            VmRef ref = new VmRef(hostRef, id, mainClass);
            vmRefs.add(ref);
        }

        return vmRefs.toArray(new VmRef[0]);
    }

    @Override
    public TreeModel getHostVmTree() {
        return publishedTreeModel;
    }

    private Ref[] getChildren(Ref parent) {
        if (parent == null) {
            return getHosts();
        } else if (parent instanceof HostRef) {
            HostRef host = (HostRef) parent;
            return getVms(host);
        }
        return new Ref[0];
    }

    @Override
    public void setHostVmTreeFilter(String filter) {
        this.filterText = filter;
        doUpdateTreeAsync();
    }

    public void doUpdateTreeAsync() {
        BackgroundTreeModelWorker worker = new BackgroundTreeModelWorker(this, publishedTreeModel, publishedRoot);
        worker.execute();
    }

    /**
     * Updates a TreeModel in the background in an Swing EDT-safe manner.
     */
    private static class BackgroundTreeModelWorker extends SwingWorker<DefaultMutableTreeNode, Void> {

        private final DefaultTreeModel treeModel;
        private MainWindowFacadeImpl facade;
        private DefaultMutableTreeNode treeRoot;

        public BackgroundTreeModelWorker(MainWindowFacadeImpl facade, DefaultTreeModel model, DefaultMutableTreeNode root) {
            this.facade = facade;
            this.treeModel = model;
            this.treeRoot = root;
        }

        @Override
        protected DefaultMutableTreeNode doInBackground() throws Exception {
            DefaultMutableTreeNode root = new DefaultMutableTreeNode();
            List<HostRef> hostsInRemoteModel = Arrays.asList(facade.getHosts());
            buildSubTree(root, hostsInRemoteModel, facade.filterText);
            return root;
        }

        private boolean buildSubTree(DefaultMutableTreeNode parent, List<? extends Ref> objectsInRemoteModel, String filter) {
            boolean subTreeMatches = false;
            for (Ref inRemoteModel : objectsInRemoteModel) {
                DefaultMutableTreeNode inTreeNode = new DefaultMutableTreeNode(inRemoteModel);

                boolean shouldInsert = false;
                if (filter == null || inRemoteModel.matches(filter)) {
                    shouldInsert = true;
                }

                List<Ref> children = Arrays.asList(facade.getChildren(inRemoteModel));
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

        private void syncTree(DefaultMutableTreeNode sourceRoot, DefaultTreeModel targetModel, DefaultMutableTreeNode targetNode) {
            List<DefaultMutableTreeNode> sourceChildren = Collections.list(sourceRoot.children());
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

    private static void printTree(PrintStream out, TreeNode node, int depth) {
        out.println(StringUtils.repeat("  ", depth) + node.toString());
        for (TreeNode child : (List<TreeNode>) Collections.list(node.children())) {
            printTree(out, child, depth + 1);
        }
    }

}
