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

package com.redhat.thermostat.client.swing.components.experimental;

import java.awt.Paint;

import javax.swing.JComponent;
import javax.swing.UIManager;

/**
 * A component that allows specifying a time range to select
 */
public class TimelineIntervalSelector extends JComponent {

    private static final String uiClassID = "TimelineIntervalSelectorUI";

    private TimelineIntervalSelectorModel model;

    private Paint linePaint;

    private Paint fillPaint;

    public TimelineIntervalSelector() {
        model = new TimelineIntervalSelectorModel();

        updateUI();
    }

    public TimelineIntervalSelectorModel getModel() {
        return model;
    }

    public void setUI(TimelineIntervalSelectorUI ui) {
        super.setUI(ui);
    }

    @Override
    public void updateUI() {
        if (UIManager.get(getUIClassID()) != null) {
            setUI((TimelineIntervalSelectorUI) UIManager.getUI(this));
        } else {
            setUI(new TimelineIntervalSelectorUIBasic());
        }
    }

    public TimelineIntervalSelectorUI getUI() {
        return (TimelineIntervalSelectorUI) ui;
    }

    @Override
    public String getUIClassID() {
        return uiClassID;
    }

    public void setSelectionLinePaint(Paint paint) {
        this.linePaint = paint;
    }

    public Paint getSelectionLinePaint() {
        return this.linePaint;
    }

    public void setSelectionFillPaint(Paint paint) {
        this.fillPaint = paint;
    }

    public Paint getSelectionFillPaint() {
        return this.fillPaint;
    }
}

