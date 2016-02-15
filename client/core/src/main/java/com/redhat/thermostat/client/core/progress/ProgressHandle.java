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

package com.redhat.thermostat.client.core.progress;

import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.shared.locale.LocalizedString;

import java.util.Objects;
import java.util.UUID;

/**
 * Handle that represents the current progress in performing a certain task.
 * The progress can be undefined or defined.
 * 
 * <br /><br />
 * 
 * A {@link ProgressHandle} has a name property and a task property. the name
 * is fixed for the whole running time of the progress and identify the
 * {@link ProgressHandle} action as a whole. Task name represent a single step
 * and can be changed.
 * 
 * For example, a UI client that downloads a file over the network and then
 * copy its content into a local database may set the {@link ProgressHandle}
 * UI clients as "Performing File Copy" and the task as "Downloading file".
 * Once the first task is complete, it may then change the task to
 * "Copying file".
 * 
 * <br /><br />
 * 
 * UI clients are free to decide how to use this information to better match
 * their User Interface Framework specification, for example the task or even
 * progress may be hidden until the user expand the status notification area.  
 */
public class ProgressHandle {

    public enum Status {
        STARTED,
        STOPPED,
        TASK_CHANGED,
        DETERMINATE_STATUS_CHANGED,
        PROGRESS_CHANGED,
        BOUNDS_CHANGED,
    }
    
    private UUID id;
    
    private final ActionNotifier<ProgressHandle.Status> notifier;
    
    private LocalizedString name;
    private LocalizedString task;
    
    private boolean indeterminate;
    
    private int currentProgress;
    private Range<Integer> range;
    
    /**
     * Create a new {@link ProgressHandle} with the given name and task set
     * as {@link LocalizedString#EMPTY_STRING}.
     */
    public ProgressHandle(LocalizedString name) {
        id = UUID.randomUUID();
        
        this.name = name;
        this.task = LocalizedString.EMPTY_STRING;
        this.range = new Range<>(0, 100);
        this.indeterminate = true;
        this.currentProgress = 0;
        
        notifier = new ActionNotifier<>(this);
    }

    /**
     * Gets the task {@link LocalizedString} currently associated with this
     * {@link ProgressHandle}.
     */
    public LocalizedString getTask() {
        return task;
    }
    
    /**
     * Sets the task currently associated 
     */
    public void setTask(LocalizedString task) {
        this.task = task;
        notifier.fireAction(Status.TASK_CHANGED, task);
    }
    
    public void setIndeterminate(boolean indeterminate) {
        this.indeterminate = indeterminate;
        notifier.fireAction(Status.DETERMINATE_STATUS_CHANGED, Boolean.valueOf(this.indeterminate));
    }

    public Range<Integer> getRange() {
        return range;
    }
    
    public void setRange(Range<Integer> range) {
        this.range = range;
        notifier.fireAction(Status.BOUNDS_CHANGED, this.range);
    }
    
    public void setProgress(int currentProgress) {
        int min = range.getMin().intValue();
        int max = range.getMax().intValue();
        
        if (currentProgress < min) {
            currentProgress = min;
        } else if (currentProgress > max) {
            currentProgress = max;
        }
        
        this.currentProgress = currentProgress;
        notifier.fireAction(Status.PROGRESS_CHANGED, Integer.valueOf(this.currentProgress));
    }
    
    public boolean isIndeterminate() {
        return indeterminate;
    }

    public int getProgress() {
        return currentProgress;
    }
    
    public LocalizedString getName() {
        return name;
    }

    public void start() {
        notifier.fireAction(Status.STARTED);
    }
    
    public void stop() {
        notifier.fireAction(Status.STOPPED);
    }

    public void runTask(Runnable task) {
        try {
            this.start();
            task.run();
        } finally {
            this.stop();
        }
    }

    public void addProgressListener(ActionListener<ProgressHandle.Status> listener) {
        notifier.addActionListener(listener);
    }
    
    public void removeProgressListener(ActionListener<ProgressHandle.Status> listener) {
        notifier.removeActionListener(listener);
    }

    @Override
    public String toString() {
        return name.getContents();
    }

    @Override
    public final int hashCode() {
        int hash = 5;
        hash = 53 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ProgressHandle other = (ProgressHandle) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        return true;
    }
}

