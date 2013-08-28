/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.client.swing.components;

import java.awt.Paint;

import javax.swing.JComponent;
import javax.swing.UIManager;

public class EventTimeline extends JComponent {

    private static final String uiClassID = "EventTimelineUI";

    private EventTimelineModel model = new EventTimelineModel();

    private Paint selectionEdgePaint = null;
    private Paint selectionFillPaint = null;
    private Paint eventPaint;

    public EventTimeline() {
        updateUI();
    }

    public void setUI(EventTimelineUI newUI) {
        super.setUI(newUI);
    }

    @Override
    public void updateUI() {
        if (UIManager.get(getUIClassID()) != null) {
            setUI((EventTimelineUI) UIManager.getUI(this));
        } else {
            setUI(new BasicEventTimelineUI());
        }
    }

    @Override
    public String getUIClassID() {
        return uiClassID;
    }

    public EventTimelineModel getModel() {
        return model;
    }

    public Paint getSelectionEdgePaint() {
        return selectionEdgePaint;
    }

    public void setSelectionEdgePaint(Paint edgePaint) {
        this.selectionEdgePaint = edgePaint;
    }

    public Paint getSelectionFillPaint() {
        return selectionFillPaint;
    }

    public void setSelectionFillPaint(Paint fillPaint) {
        this.selectionFillPaint = fillPaint;
    }

    public void setEventPaint(Paint eventPaint) {
        this.eventPaint = eventPaint;
    }

    public Paint getEventPaint() {
        return eventPaint;
    }


}
