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
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.vm.heap.analysis.client.core.HeapObjectUI;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectDetailsView;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectDetailsView.ObjectAction;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectDetailsView.ObjectReferenceCallback;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectDetailsViewProvider;
import com.redhat.thermostat.vm.heap.analysis.client.core.ObjectRootsViewProvider;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDump;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.JavaClass;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.JavaField;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.JavaHeapObject;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.JavaHeapObjectVisitor;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.JavaThing;

public class ObjectDetailsController {

    private ApplicationService appService;
    private HeapDump heapDump;
    private ObjectDetailsView view;
    private ObjectRootsViewProvider viewProvider;

    public ObjectDetailsController(ApplicationService appService, HeapDump heapDump, ObjectDetailsViewProvider viewProvider, ObjectRootsViewProvider rootsViewProvider) {
        this.appService = appService;
        this.heapDump = heapDump;
        this.viewProvider = rootsViewProvider;

        view = viewProvider.createView();
        addListenersToView();
    }

    private void addListenersToView() {

        view.addObjectActionListener(new ActionListener<ObjectAction>() {
            @Override
            public void actionPerformed(ActionEvent<ObjectAction> actionEvent) {
                switch (actionEvent.getActionId()) {
                case SEARCH:
                    searchForObject();
                    break;
                case GET_OBJECT_DETAIL:
                    showObjectInfo();
                    break;
                case SHOW_ROOT_TO_GC:
                    HeapObjectUI heapObj = (HeapObjectUI) actionEvent.getPayload();
                    showPathToGC(heapObj);
                    break;
                default:
                    throw new NotImplementedException("unknown action fired by " + actionEvent.getSource());
                }

            }
        });

        view.addObjectReferenceCallback(new ObjectReferenceCallback() {
            @Override
            public Collection<HeapObjectUI> getReferrers(HeapObjectUI obj) {
                JavaHeapObject heapObject = heapDump.findObject(obj.objectId);

                @SuppressWarnings("unchecked")
                Enumeration<JavaThing> referrers = heapObject.getReferers();

                List<HeapObjectUI> objects = new ArrayList<>();
                while (referrers.hasMoreElements()) {
                    // Theoretically, referrer.nextElement() can return any
                    // subclass of JavaThing. But after parsing the heap (which
                    // we do before displaying it in UI) only JavaHeapObject
                    // instances are returned.
                    heapObject = (JavaHeapObject) referrers.nextElement();
                    objects.add(new HeapObjectUI(heapObject.getIdString(), PrintObjectUtils.objectToString(heapObject)));
                }
                return objects;
            }

            @Override
            public Collection<HeapObjectUI> getReferences(HeapObjectUI obj) {
                final JavaHeapObject heapObject = heapDump.findObject(obj.objectId);

                final List<JavaHeapObject> references = new ArrayList<>();

                JavaHeapObjectVisitor v = new JavaHeapObjectVisitor() {

                    @Override
                    public void visit(JavaHeapObject other) {
                        references.add(other);
                    }

                    @Override
                    public boolean mightExclude() {
                        /* we never return true in exclude */
                        return false;
                    }

                    @Override
                    public boolean exclude(JavaClass clazz, JavaField f) {
                        /* visit every field in every java class */
                        return false;
                    }
                };

                heapObject.visitReferencedObjects(v);

                List<HeapObjectUI> objects = new ArrayList<>();
                for (JavaHeapObject ref: references) {
                    objects.add(new HeapObjectUI(ref.getIdString(), PrintObjectUtils.objectToString(ref)));
                }
                return objects;
            }
        });
    }

    private void searchForObject() {
        final String searchText = view.getSearchText();
        if (searchText == null || searchText.trim().isEmpty()) {
            view.setMatchingObjects(Collections.<HeapObjectUI>emptySet());
            return;
        }

        appService.getApplicationExecutor().execute(new Runnable() {

            @Override
            public void run() {
                Collection<String> objectIds = heapDump.wildcardSearch(searchText);

                List<HeapObjectUI> toDisplay = new ArrayList<>();
                for (String id: objectIds) {
                    JavaHeapObject heapObject = heapDump.findObject(id);
                    toDisplay.add(new HeapObjectUI(id, PrintObjectUtils.objectToString(heapObject)));
                }
                view.setMatchingObjects(toDisplay);
            }
        });

    }

    private void showObjectInfo() {
        HeapObjectUI matchingObject = view.getSelectedMatchingObject();
        if (matchingObject == null) {
            return;
        }
        String objectId = matchingObject.objectId;
        JavaHeapObject object = heapDump.findObject(objectId);
        view.setObjectDetails(object);
    }

    private void showPathToGC(HeapObjectUI targetObject) {
        JavaHeapObject heapObject = heapDump.findObject(targetObject.objectId);

        ObjectRootsController controller = new ObjectRootsController(heapDump, heapObject, viewProvider);
        controller.show();
    }

    public ObjectDetailsView getView() {
        return view;
    }
}

