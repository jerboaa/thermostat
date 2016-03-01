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

package com.redhat.thermostat.notes.client.core;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.notes.common.Note;
import com.redhat.thermostat.notes.common.NoteDAO;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.Ref;

public abstract class NotesController<R extends Ref, N extends Note, D extends NoteDAO<R, N>> implements InformationServiceController<R> {

    protected static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    protected Clock clock;
    protected ApplicationService appSvc;
    protected R ref;
    protected D dao;
    protected NotesView view;

    private SortedSet<N> models;
    private SortedSet<N> modelSnapshot;
    private Set<N> addedSet;
    private Set<N> updatedSet;
    private Set<N> removedSet;
    private Timer syncTimer;
    private Timer autoRefreshTimer;

    public NotesController(Clock clock, final ApplicationService appSvc, final R ref, final D dao, NotesViewProvider viewProvider) {
        this.clock = clock;
        this.appSvc = appSvc;
        this.ref = ref;
        this.dao = dao;
        this.view = viewProvider.createView();

        NoteIdComparator<N> noteIdComparator = new NoteIdComparator<>();
        models = new TreeSet<>(noteIdComparator);
        modelSnapshot = new TreeSet<>(noteIdComparator);
        addedSet = new HashSet<>();
        updatedSet = new HashSet<>();
        removedSet = new HashSet<>();

        syncTimer = appSvc.getTimerFactory().createTimer();
        syncTimer.setSchedulingType(Timer.SchedulingType.FIXED_RATE);
        syncTimer.setTimeUnit(TimeUnit.MILLISECONDS);
        syncTimer.setInitialDelay(0l);
        syncTimer.setDelay(250l);

        autoRefreshTimer = appSvc.getTimerFactory().createTimer();
        autoRefreshTimer.setAction(new AutoRefreshTask());
        autoRefreshTimer.setInitialDelay(0l);
        autoRefreshTimer.setDelay(30);
        autoRefreshTimer.setTimeUnit(TimeUnit.SECONDS);
        autoRefreshTimer.setSchedulingType(Timer.SchedulingType.FIXED_RATE);

        this.view.addNoteActionListener(new ActionListener<NotesView.NoteAction>() {
            @Override
            public void actionPerformed(final ActionEvent<NotesView.NoteAction> actionEvent) {
                switch (actionEvent.getActionId()) {
                    /* remote-storage operations */
                    case REMOTE_REFRESH: {
                        appSvc.getApplicationExecutor().submit(new Runnable() {
                            @Override
                            public void run() {
                                sync();
                            }
                        });
                        break;
                    }
                    /* local operations */
                    case LOCAL_ADD: {
                        appSvc.getApplicationExecutor().submit(new Runnable() {
                            @Override
                            public void run() {
                                sync(new Runnable() {
                                    @Override
                                    public void run() {
                                        localAddNewNote();
                                    }
                                });
                            }
                        });
                        break;
                    }
                    case LOCAL_SAVE: {
                        appSvc.getApplicationExecutor().submit(new Runnable() {
                            @Override
                            public void run() {
                                String noteId = /* tag = */ (String) actionEvent.getPayload();
                                localSaveNote(noteId);
                            }
                        });
                        break;
                    }
                    case LOCAL_DELETE: {
                        appSvc.getApplicationExecutor().submit(new Runnable() {
                            @Override
                            public void run() {
                                String noteId = /* tag = */ (String) actionEvent.getPayload();
                                localDeleteNote(noteId);
                                sync();
                            }
                        });
                        break;
                    }
                    default:
                        throw new AssertionError("Unknown acton event: " + actionEvent);
                }
            }
        });

        view.addActionListener(new ActionListener<BasicView.Action>() {
            @Override
            public void actionPerformed(ActionEvent<BasicView.Action> actionEvent) {
                switch (actionEvent.getActionId()) {
                    case HIDDEN:
                        autoRefreshTimer.stop();
                        appSvc.getApplicationExecutor().submit(new Runnable() {
                            @Override
                            public void run() {
                                sendLocalChangesToStorage();
                                addedSet.clear();
                                updatedSet.clear();
                                removedSet.clear();
                                models.clear();
                            }
                        });
                        break;
                    case VISIBLE:
                        autoRefreshTimer.start();
                        break;
                    default:
                        throw new AssertionError("Unknown action event: " + actionEvent);
                }
            }
        });
    }

    @Override
    public UIComponent getView() {
        return view;
    }

    @Override
    public LocalizedString getLocalizedName() {
        return translator.localize(LocaleResources.VIEW_NAME);
    }

    protected void sync(Runnable onComplete) {
        syncTimer.stop();
        syncTimer.setAction(new SyncTask(onComplete));
        syncTimer.start();
    }

    protected void sync() {
        sync(new EmptyRunnable());
    }

    protected void retrieveNotesFromStorage() {
        Utils.assertNotInEdt();

        view.setBusy(true);

        addedSet.clear();
        updatedSet.clear();
        removedSet.clear();
        models.clear();
        models.addAll(dao.getFor(ref));
        localUpdateNotesInView();

        view.setBusy(false);
    }

    protected void sendLocalChangesToStorage() {
        view.setBusy(true);

        List<N> remoteModels = dao.getFor(ref);

        updatedSet.removeAll(removedSet);
        addedSet.removeAll(removedSet);
        addedSet.removeAll(updatedSet);

        assertDistinct(addedSet, updatedSet);
        assertDistinct(updatedSet, removedSet);
        assertDistinct(removedSet, addedSet);

        Set<N> justSent = new HashSet<>();

        for (N note : addedSet) {
            N remote = findById(remoteModels, note.getId());
            if (remote == null) {
                if (!justSent.contains(note)) {
                    dao.add(note);
                    addedSet.add(note);
                    justSent.add(note);
                }
            } else {
                if (remote.getTimeStamp() > note.getTimeStamp()) {
                    // if remote already contains a note with the same ID and a newer timestamp, what should we do?
                    // maybe create a new note, copy the timestamp and content, and add that to remote?
                } else {
                    // should we overwrite with an update, or also create a copy here? If this is a new note
                    // addition locally then if remote already has a note with this ID, it seems like it's an
                    // unintentionally ID collision.
                    if (!justSent.contains(note)) {
                        dao.update(note);
                        justSent.add(note);
                    }
                }
            }
        }

        for (N note : updatedSet) {
            N remote = findById(remoteModels, note.getId());
            if (remote == null) {
                if (!justSent.contains(note)) {
                    dao.add(note);
                    addedSet.add(note);
                    justSent.add(note);
                }
            } else if (note.getTimeStamp() > remote.getTimeStamp()) {
                if (!justSent.contains(note)) {
                    dao.update(note);
                    justSent.add(note);
                }
            }
        }

        for (N note : removedSet) {
            dao.remove(note);
        }

        view.setBusy(false);
    }

    /** Add a new note */
    protected void localAddNewNote() {
        long timeStamp = clock.getRealTimeMillis();
        String content = "";

        N note = createNewNote(timeStamp, content);
        models.add(note);
        addedSet.add(note);

        localUpdateNotesInView();
    }

    protected abstract N createNewNote(long timeStamp, String content);

    /** Update the view to match what's in the local cache */
    protected void localUpdateNotesInView() {
        Set<N> unseen = removeById(models, modelSnapshot);
        Set<N> retained = retainById(models, modelSnapshot);
        Set<N> removed = removeById(modelSnapshot, retained);

        assertDistinct(unseen, retained);
        assertDistinct(unseen, removed);
        assertDistinct(retained, removed);

        for (Note note : removed) {
            view.remove(note);
        }

        for (Note note : retained) {
            view.update(note);
        }

        for (Note note : unseen) {
            view.add(note);
        }

        modelSnapshot.clear();
        modelSnapshot.addAll(models);
    }

    /** Update the local cache of notes to match what's in the view */
    protected void localSaveNote(String noteId) {
        long timeStamp = clock.getRealTimeMillis();

        N note = findById(models, noteId);
        if (note == null) {
            throw new AssertionError("Unable to find local note model to save");
        }
        N added = findById(addedSet, noteId);
        if (added != null) {
            addedSet.remove(added);
        }
        updatedSet.add(note);
        String oldContent = note.getContent();
        String newContent = view.getContent(noteId);
        long oldTimestamp = note.getTimeStamp();
        long newTimestamp = view.getTimeStamp(noteId);
        if (!oldContent.equals(newContent)
                || oldTimestamp != newTimestamp) {
            note.setTimeStamp(timeStamp);
            note.setContent(newContent);
            view.update(note);
        }
    }

    /** Delete a note */
    protected void localDeleteNote(String noteId) {
        N note = findById(models, noteId);
        if (note == null) {
            throw new AssertionError("Unable to find note to delete");
        }

        removedSet.add(note);
        boolean removed = models.remove(note);
        if (!removed) {
            throw new AssertionError("Deleting a note failed");
        }

        localUpdateNotesInView();
    }

    private Set<N> retainById(Set<N> source, Set<N> retained) {
        Set<N> result = new HashSet<>();
        for (N note : source) {
            if (findById(retained, note.getId()) != null) {
                result.add(note);
            }
        }
        return result;
    }

    private Set<N> removeById(Set<N> source, Set<N> removed) {
        Set<N> result = new HashSet<>(source);
        for (N note : source) {
            if (findById(removed, note.getId()) != null) {
                result.remove(note);
            }
        }
        return result;
    }

    private static <T> void assertDistinct(Collection<T> a, Collection<T> b) {
        Collection<T> overlap = new HashSet<>(a);
        overlap.retainAll(b);
        if (!overlap.isEmpty()) {
            throw new AssertionError("Collections were not distinct");
        }
    }

    private N findById(Iterable<N> notes, String id) {
        for (N note : notes) {
            if (note.getId().equals(id)) {
                return note;
            }
        }
        return null;
    }

    private class SyncTask implements Runnable {

        private boolean firstRun = true;
        private long startTime;
        private long initialCount;
        private long expectedDelta;
        private Runnable onComplete;

        public SyncTask(Runnable onComplete) {
            initialCount = dao.getCount(ref);
            expectedDelta = addedSet.size() - removedSet.size();
            this.onComplete = onComplete;
        }

        @Override
        public void run() {
            if (firstRun) {
                sendLocalChangesToStorage();
                view.setBusy(true);
                startTime = System.currentTimeMillis();
                firstRun = false;
                return;
            }
            long currentTime = System.currentTimeMillis();
            boolean timeoutElapsed = startTime + TimeUnit.SECONDS.toMillis(5l) < currentTime;
            if (expectedDelta == 0
                    || timeoutElapsed
                    || dao.getCount(ref) == (initialCount + expectedDelta)) {
                syncTimer.stop();
                view.setBusy(false);
                retrieveNotesFromStorage();
                onComplete.run();
            }
        }
    }

    private class EmptyRunnable implements Runnable {
        @Override
        public void run() {
            // intentionally empty
        }
    }

    private class AutoRefreshTask implements Runnable {
        @Override
        public void run() {
            view.actionNotifier.fireAction(NotesView.NoteAction.REMOTE_REFRESH);
        }
    }

    private class NoteIdComparator<T extends N> implements Comparator<T> {
        @Override
        public int compare(T t, T t1) {
            return Long.compare(t.getTimeStamp(), t1.getTimeStamp());
        }
    }

}
