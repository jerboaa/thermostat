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

package com.redhat.thermostat.client.swing.components.experimental;

import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

class TimelineIntervalMouseHandler extends MouseAdapter {

    static interface TimeIntervalSelectorTarget {
        void setCursor(Cursor cursor);
        int getSelectionMargin();
        int getLeftSelectionPosition();
        int getRightSelectionPosition();
        void updateSelectionPosition(int left, int right);
    }

    private final TimeIntervalSelectorTarget target;

    private boolean moving = false;
    private boolean movingLeft = false;
    private boolean movingRight = false;

    private int oldX = -1;
    private int oldLeft = -1;
    private int oldRight = -1;

    public TimelineIntervalMouseHandler(TimeIntervalSelectorTarget target) {
        this.target = target;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (Math.abs(e.getX() - target.getLeftSelectionPosition()) < target.getSelectionMargin()) {
            target.setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
        } else if (Math.abs(e.getX() - target.getRightSelectionPosition()) < target.getSelectionMargin()) {
            target.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
        } else if ((e.getX() > target.getSelectionMargin() + target.getLeftSelectionPosition()) && (e.getX() < target.getRightSelectionPosition() - target.getSelectionMargin())) {
            target.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        } else {
            target.setCursor(Cursor.getDefaultCursor());
        }

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (moving || movingLeft || movingRight) {

            int newLeft = oldLeft;
            int newRight = oldRight;
            if (movingLeft) {
                newLeft = e.getX();
            } else if (movingRight) {
                newRight = e.getX();
            } else if (moving) {
                long delta = e.getX() - oldX;
                newLeft += delta;
                newRight += delta;
            }

            target.updateSelectionPosition(newLeft, newRight);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        moving = movingLeft = movingRight = false;
        oldLeft = oldRight = oldX = -1;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (Math.abs(e.getX() - target.getLeftSelectionPosition()) < target.getSelectionMargin()) {
            movingLeft = true;
        } else if (Math.abs(e.getX() - target.getRightSelectionPosition()) < target.getSelectionMargin()) {
            movingRight = true;
        } else if ((e.getX() > target.getLeftSelectionPosition() + target.getSelectionMargin()) && (e.getX() < target.getRightSelectionPosition() - target.getSelectionMargin())) {
            moving = true;
        }
        oldLeft = target.getLeftSelectionPosition();
        oldRight = target.getRightSelectionPosition();
        oldX = e.getX();
    }

    // TODO implement wheel scrolling
}

