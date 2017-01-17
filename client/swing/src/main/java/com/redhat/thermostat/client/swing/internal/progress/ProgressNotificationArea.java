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

package com.redhat.thermostat.client.swing.internal.progress;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.redhat.thermostat.client.core.progress.ProgressHandle;
import com.redhat.thermostat.client.core.progress.ProgressHandle.Status;
import com.redhat.thermostat.client.swing.components.FontAwesomeIcon;
import com.redhat.thermostat.client.swing.components.Icon;
import com.redhat.thermostat.client.swing.components.ShadowLabel;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.model.Range;

@SuppressWarnings("serial")
public class ProgressNotificationArea extends JPanel {
    
    private ProgressHandle runningTask;
    private ShadowLabel taskLabel;
    private Icon moreTasksIcon;

    private boolean hasMore;
    
    public ProgressNotificationArea() {
        setLayout(new BorderLayout(0, 0));
        
        taskLabel = new ShadowLabel();
        taskLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        taskLabel.setVerticalAlignment(SwingConstants.CENTER);
        
        moreTasksIcon = new FontAwesomeIcon('\uf0d8', 12);
    }

    private void handleAction(ActionEvent<Status> actionEvent, JProgressBar progressBar) {
        switch(actionEvent.getActionId()) {
        case DETERMINATE_STATUS_CHANGED:
            progressBar.setIndeterminate(((Boolean) actionEvent.getPayload()).booleanValue());
            break;

        case BOUNDS_CHANGED: {
            @SuppressWarnings("unchecked")
            Range<Integer> range = (Range<Integer>) actionEvent.getPayload();
            progressBar.setMinimum(range.getMin().intValue());
            progressBar.setMaximum(range.getMax().intValue());
            
        } break;
        
        case PROGRESS_CHANGED:
            progressBar.setValue(((Integer) actionEvent.getPayload()).intValue());
            break;
            
        default:
            break;
        }
    }
    
    public void setRunningTask(final ProgressHandle handle) {
        removeAll();
                
        taskLabel.setText(handle.getName().getContents());
        if (hasMore) {
            taskLabel.setIcon(moreTasksIcon);
        } else {
            taskLabel.setIcon(null);
        }
        
        add(taskLabel, BorderLayout.CENTER);
        
        final JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(handle.isIndeterminate());
        add(progressBar, BorderLayout.EAST);

        progressBar.setName(handle.getName().getContents());
        
        handle.addProgressListener(new ActionListener<ProgressHandle.Status>() {
            @Override
            public void actionPerformed(final ActionEvent<Status> actionEvent) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        handleAction(actionEvent, progressBar);
                    }
                });
            }
        });
        
        runningTask = handle;
        
        revalidate();
        repaint();
    }
    
    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
        if (hasMore) {
            taskLabel.setIcon(moreTasksIcon);
        } else {
            taskLabel.setIcon(null);
        }
    }
    
    public ProgressHandle getRunningTask() {
        return runningTask;
    }

    public void reset() {
        removeAll();
        revalidate();
        repaint();
        runningTask = null;
        hasMore = false;
    }
}

