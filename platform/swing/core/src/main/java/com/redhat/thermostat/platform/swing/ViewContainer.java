/*
 * Copyright 2012-2015 Red Hat, Inc.
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

import com.redhat.thermostat.beans.property.ChangeListener;
import com.redhat.thermostat.beans.property.ObservableValue;
import com.redhat.thermostat.platform.application.swing.internal.ComponentVisibilityDispatcher;
import com.redhat.thermostat.platform.mvc.View;
import com.redhat.thermostat.animation.Animation;
import com.redhat.thermostat.platform.swing.components.ContentPane;

/**
 */
public class ViewContainer extends View  {

    protected ContentPane contentPane;
    private ComponentVisibilityDispatcher visibilityDispatcher;

    @Override
    public final void create() {
        super.create();
        visibilityDispatcher = new ComponentVisibilityDispatcher();
        contentPane = createContentPane();
        postCreate(contentPane);
    }

    protected ContentPane createContentPane() {
        return new ContentPane();
    }

    protected void postCreate(ContentPane contentPane) {}

    public final void add(ContentProvider content) {
        add(content, null);
    }

    public final void add(ContentProvider content, Animation animation) {
        add(content, null, -1, animation);
    }

    public final void remove(ContentProvider content) {
        remove(content, null);
    }

    public final void remove(final ContentProvider content, final Animation animation) {

        if (animation != null) {
            animation.statusProperty().addListener(new ChangeListener<Animation.Status>() {
                @Override
                public void changed(ObservableValue<? extends Animation.Status> status,
                                    Animation.Status oldValue,
                                    Animation.Status newValue)
                {
                    if (newValue.equals(Animation.Status.STOPPED)) {
                        clean(content);
                        animation.statusProperty().removeListener(this);
                    }
                }
            });
            animation.play();

        } else {
            clean(content);
        }
    }

    private void clean(ContentProvider content) {
        visibilityDispatcher.deregister(content);
        contentPane.remove(content.getContent());
    }

    public final void add(ContentProvider content, Object constraints, int index, Animation animation) {
        visibilityDispatcher.register(content);
        addImpl(content, constraints, index, animation);
    }

    protected void addImpl(ContentProvider content, Object constraints, int index, Animation animation) {
        contentPane.add(content.getContent(), constraints, index);
        if (animation != null) {
            animation.play();
        }
    }
}
