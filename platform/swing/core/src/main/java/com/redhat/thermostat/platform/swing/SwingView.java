/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.platform.swing;
import com.redhat.thermostat.platform.mvc.View;
import com.redhat.thermostat.platform.swing.components.ContentPane;

import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class SwingView extends View  {

    protected ContentPane contentPane;
    private ComponentVisibilityListener listener;

    @Override
    public final void create() {

        preCreate();

        super.create();

        contentPane = createContentPane();
        listener = new ComponentVisibilityListener(contentPane, this);
        contentPane.addHierarchyListener(listener);

        postCreate(contentPane);
    }

    protected ContentPane createContentPane() {
        return new ContentPane();
    }

    @Override
    public final void destroy() {
        preDestroy();

        super.destroy();

        contentPane.removeHierarchyListener(listener);
        postDestroy();
    }

    protected void preDestroy() {}
    protected void postDestroy() {}
    protected void preCreate() {}
    protected void postCreate(ContentPane contentPane) {}

    public final void add(SwingView content) {
        add(content, null, -1);
    }

    public final void remove(final SwingView content) {
        clean(content);
    }

    private void clean(SwingView content) {
        removeImpl(content.contentPane);
    }

    public final void add(SwingView content, Object constraints, int index) {
        addImpl(content.contentPane, constraints, index);
    }

    protected void addImpl(ContentPane content, Object constraints, int index) {
        contentPane.add(content, constraints, index);
    }

    protected void removeImpl(ContentPane content) {
        contentPane.remove(content);
    }

    private class ComponentVisibilityListener implements HierarchyListener {

        private ContentPane contentPane;
        private SwingView view;

        public ComponentVisibilityListener(ContentPane contentPane, SwingView view) {
            this.contentPane = contentPane;
            this.view = view;
        }

        @Override
        public void hierarchyChanged(HierarchyEvent e) {
            if (contentPane == null) {
                return;
            }

            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0)  {
                view.showingProperty().set(e.getComponent().isShowing());
            }
        }
    }
}
