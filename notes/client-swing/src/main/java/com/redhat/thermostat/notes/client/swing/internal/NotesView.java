/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.notes.client.swing.internal;

import java.awt.AWTEvent;
import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.beans.PropertyChangeEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.LayerUI;

import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.client.swing.EdtHelper;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.ActionButton;
import com.redhat.thermostat.client.swing.components.FontAwesomeIcon;
import com.redhat.thermostat.client.swing.components.HeaderPanel;
import com.redhat.thermostat.client.swing.components.ThermostatScrollPane;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.notes.common.Note;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;

/** SwingComponent serves as a tag for SwingClient to use this view */
public class NotesView implements UIComponent, SwingComponent {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private HeaderPanel container;

    private JLayer<JPanel> containerCover;
    private BusyLayerUI busyLayer;

    private ThermostatScrollPane notesScrollPane;
    private VerticalScrollablePanel notesContainer;
    private JPanel notesAndToolsContainer;

    private ActionButton refreshButton;
    private ActionButton saveButton;

    public enum Action {
        REMOTE_REFRESH,
        REMOTE_SAVE,

        LOCAL_ADD,
        LOCAL_DELETE,
        LOCAL_SAVE,
    }

    private ActionNotifier<Action> actionNotifier = new ActionNotifier<>(this);

    private Map<String, NotePanel> tagToPanel;

    public NotesView() {
        Utils.assertInEdt();

        tagToPanel = new HashMap<>();

        container = new HeaderPanel(translator.localize(LocaleResources.TAB_NAME));

        refreshButton = createToolbarButton(
                translator.localize(LocaleResources.NOTES_REFRESH),
                '\uf021',
                Action.REMOTE_REFRESH);
        container.addToolBarButton(refreshButton);

        saveButton = createToolbarButton(
                translator.localize(LocaleResources.NOTES_SAVE),
                '\uf0c7',
                Action.REMOTE_SAVE);
        container.addToolBarButton(saveButton);

        notesAndToolsContainer = new JPanel();
        BoxLayout contentAndToolsLayout = new BoxLayout(notesAndToolsContainer, BoxLayout.PAGE_AXIS);
        notesAndToolsContainer.setLayout(contentAndToolsLayout);

        notesContainer = new VerticalScrollablePanel();
        BoxLayout layout = new BoxLayout(notesContainer, BoxLayout.Y_AXIS);
        notesContainer.setLayout(layout);

        notesScrollPane = new ThermostatScrollPane(notesContainer);
        int padding = Constants.PADDING;
        notesScrollPane.setBorder(new EmptyBorder(padding, padding, padding, padding));
        notesAndToolsContainer.add(notesScrollPane);

        JButton addNewNoteButton = new JButton("Add");
        addNewNoteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                handleActionAsync(Action.LOCAL_ADD);
            }
        });
        JPanel buttonContainer = new JPanel(new FlowLayout(FlowLayout.LEADING));
        buttonContainer.add(addNewNoteButton);
        notesAndToolsContainer.add(buttonContainer);

        busyLayer = new BusyLayerUI();
        containerCover = new JLayer<JPanel>(notesAndToolsContainer, busyLayer);

        container.setContent(containerCover);
    }

    private ActionButton createToolbarButton(LocalizedString description, char iconId, final Action action) {
        Icon icon = new FontAwesomeIcon(iconId, Constants.TEXT_SIZE);
        ActionButton button = new ActionButton(icon);
        button.setToolTipText(description.getContents());
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                handleActionAsync(action);
            }
        });
        return button;
    }

    @Override
    public Component getUiComponent() {
        return container;
    }

    public ActionNotifier<Action> getNotifier() {
        return actionNotifier;
    }

    public void setBusy(final boolean busy) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                busyLayer.setBusy(busy);
                containerCover.repaint();
                refreshButton.setEnabled(!busy);
                saveButton.setEnabled(!busy);
            }
        });
    }

    public void clearAll() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<String, NotePanel> entry : tagToPanel.entrySet()) {
                    notesContainer.remove(entry.getValue());
                }
                tagToPanel.clear();
                notesAndToolsContainer.revalidate();
            }
        });
    }

    public void add(final Note note) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                NotePanel widget = new NotePanel(note, actionNotifier);
                tagToPanel.put(note.getId(), widget);
                notesContainer.add(widget);
                notesAndToolsContainer.revalidate();
            }
        });
    }

    public String getContent(final String tag) {
        try {
            return new EdtHelper().callAndWait(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return tagToPanel.get(tag).getContent();
                }
            });
        } catch (InvocationTargetException | InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    public void setTimeStamp(final String tag, final long timeStamp) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                tagToPanel.get(tag).setTimeStamp(timeStamp);
            }
        });
    }

    private void handleActionAsync(final Action action) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>(){
            @Override
            protected Void doInBackground() throws Exception {
                actionNotifier.fireAction(action);
                return null;
            }
        };
        worker.execute();
    }

    /**
     * A {@link JPanel} that only allows vertical scrolling.
     */
    static class VerticalScrollablePanel extends JPanel implements Scrollable {

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return super.getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            //Unit scroll 1/10th of the panel's height.
            //Used when scrolling with scrollbar arrows.
            return (int)(this.getPreferredSize().getHeight() / 10);
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            //Block scroll 1/5th of the panel's height.
            //Used when scrolling with mouse-wheel clicks.
            return (int)(this.getPreferredSize().getHeight() / 5);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /**
     * A {@link LayerUI} that shows a busy progress indicator over a set of
     * components and prevent any user interaction with the components.
     *
     * @see #setBusy(boolean)
     */
    static class BusyLayerUI extends LayerUI<JPanel> implements ActionListener {

        private static final int SPINNER_FONT_SIZE = 20;
        private static final int MAX_STEPS = 20;
        private static final String PROPERTY_TICK = "tick";

        private boolean busy;
        private Image image;
        private int step;
        private Timer timer;

        public BusyLayerUI() {
            char spinnerChar = '\uf110'; // fa-spinner
            FontAwesomeIcon spinner = new FontAwesomeIcon(spinnerChar, SPINNER_FONT_SIZE);
            image = spinner.getImage();
            step = 0;
            timer = new Timer(100, this);
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            int w = c.getWidth();
            int h = c.getHeight();

            if (w == 0 || h == 0) {
                return;
            }

            super.paint(g, c);

            Graphics2D g2 = (Graphics2D) g.create();

            if (busy) {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                Composite originalComposite = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5f));
                g2.fillRect(0, 0, w, h);
                g2.setComposite(originalComposite);

                g2.translate(w/2, h/2);
                int x = image.getWidth(null) / 2;
                int y = image.getHeight(null) / 2;
                g2.rotate(Math.PI * 2 * step / MAX_STEPS);
                g2.drawImage(image, -x, -y, null);
            }

            g2.dispose();
        }

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            ((JLayer<?>) c).setLayerEventMask(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
        }

        @Override
        public void uninstallUI(JComponent c) {
            super.uninstallUI(c);
            ((JLayer<?>) c).setLayerEventMask(0);
        }

        /** Enables or disables the progress indicator */
        public void setBusy(boolean busy) {
            this.busy = busy;
            if (busy) {
                timer.start();
            } else {
                timer.stop();
            }
        }

        @Override
        public void eventDispatched(AWTEvent e, JLayer<? extends JPanel> l) {
            if (busy && e instanceof InputEvent) {
                ((InputEvent) e).consume();
            }
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (busy) {
                step = (step + 1) % MAX_STEPS;
            }
            firePropertyChange(PROPERTY_TICK, 0, 1);
        }

        @Override
        public void applyPropertyChange(PropertyChangeEvent evt, JLayer<? extends JPanel> l) {
            if (PROPERTY_TICK.equals(evt.getPropertyName())) {
                l.repaint();
            }
        }
    }
}
