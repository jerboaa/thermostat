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

package com.redhat.thermostat.vm.heap.analysis.client.core.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapObjectUI;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectRootsView;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectRootsView.Action;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectRootsViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.locale.LocaleResources;
import com.redhat.thermostat.vm.heap.analysis.command.FindRoot;
import com.redhat.thermostat.vm.heap.analysis.command.HeapPath;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDump;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.JavaHeapObject;

import static com.redhat.thermostat.common.utils.IteratorUtils.head;

public class ObjectRootsController {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private final ObjectRootsView view;

    private final HeapDump heapDump;
    private final JavaHeapObject heapObject;
    private final FindRoot rootFinder;

    public ObjectRootsController(HeapDump dump, JavaHeapObject heapObject, ObjectRootsViewProvider viewProvider) {
        this(dump, heapObject, new FindRoot(), viewProvider);
    }

    public ObjectRootsController(HeapDump dump, JavaHeapObject heapObject, FindRoot findRoot, ObjectRootsViewProvider viewProvider) {
        this.heapDump = dump;
        this.heapObject = heapObject;
        this.rootFinder = findRoot;

        view = viewProvider.createView();

        view.addActionListener(new ActionListener<ObjectRootsView.Action>() {

            @Override
            public void actionPerformed(ActionEvent<Action> actionEvent) {
                switch (actionEvent.getActionId()) {
                case VISIBLE:
                    view.showView();
                    break;
                case HIDDEN:
                    view.hideView();
                    break;
                case OBJECT_SELECTED:
                    HeapObjectUI obj = (HeapObjectUI) actionEvent.getPayload();
                    showObjectDetails(obj);
                    break;
                default:
                    throw new NotImplementedException("unexpected action " +
                            actionEvent.getActionId() + " recieved by " + ObjectRootsController.this.getClass().getName());
                }

            }
        });
    }

    private void showObjectDetails(HeapObjectUI uiObject) {
        JavaHeapObject obj = heapDump.findObject(uiObject.objectId);
        String space = " ";
        String newline = "\n";
        StringBuilder builder = new StringBuilder();
        builder.append(translator.localize(LocaleResources.COMMAND_OBJECT_INFO_OBJECT_ID).getContents())
                .append(space)
                .append(obj.getIdString())
                .append(newline)
                .append(translator.localize(LocaleResources.COMMAND_OBJECT_INFO_TYPE).getContents())
                .append(space)
                .append(obj.getClazz().getName())
                .append(newline)
                .append(translator.localize(LocaleResources.COMMAND_OBJECT_INFO_SIZE).getContents())
                .append(space)
                .append(String.valueOf(obj.getSize()))
                .append(" bytes")
                .append(newline)
                .append(translator.localize(LocaleResources.COMMAND_OBJECT_INFO_HEAP_ALLOCATED).getContents())
                .append(space)
                .append(String.valueOf(obj.isHeapAllocated()))
                .append(newline);

        if (obj.getRoot() != null) {
            builder.append(obj.getRoot().getDescription());
        }
        view.setObjectDetails(builder.toString());
    }


    public void show() {
        Collection<HeapPath<JavaHeapObject>> paths = rootFinder.findShortestPathsToRoot(heapObject, false);
        Iterator<HeapPath<JavaHeapObject>> iter = paths.iterator();
        HeapPath<JavaHeapObject> pathToRoot = head(iter);
        if (pathToRoot != null) {
            List<HeapObjectUI> path = new ArrayList<>();
            for (JavaHeapObject heapObj : pathToRoot) {
                path.add(new HeapObjectUI(heapObj.getIdString(), PrintObjectUtils.objectToString(heapObj)));
            }
            view.setPathToRoot(path);
        }
        view.showView();
    }

}

