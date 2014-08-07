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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JPanel;

import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.client.swing.SwingComponent;
import com.redhat.thermostat.client.swing.components.ActionButton;
import com.redhat.thermostat.client.swing.components.FontAwesomeIcon;
import com.redhat.thermostat.client.swing.components.HeaderPanel;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;

/** SwingComponent serves as a tag for SwingClient to use this view */
public class VmNotesView implements UIComponent, SwingComponent {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final int FONT_SIZE = 12;

    private HeaderPanel container;
    private JPanel contentContainer;
    private GridBagConstraints contentContainerConstraints;

    public enum Action {
        NEW,
        LOAD,
        SAVE,
        DELETE,
    }

    private ActionNotifier<Action> actionNotifier = new ActionNotifier<>(this);

    private Map<String, VmNotePanel> tagToPanel;

    public VmNotesView() {
        tagToPanel = new HashMap<>();

        container = new HeaderPanel(translator.localize(LocaleResources.VM_TAB_NAME));

        container.addToolBarButton(
                createToolbarButton(translator.localize(LocaleResources.VM_NOTES_NEW), '\uf067', Action.NEW));

        container.addToolBarButton(
                createToolbarButton(translator.localize(LocaleResources.VM_NOTES_REFRESH), '\uf021', Action.LOAD));

        container.addToolBarButton(
                createToolbarButton(translator.localize(LocaleResources.VM_NOTES_SAVE), '\uf0c7', Action.SAVE));

        JPanel rootContentContainer = new JPanel();
        rootContentContainer.setLayout(new BorderLayout());
        contentContainer = new JPanel();
        rootContentContainer.add(contentContainer, BorderLayout.NORTH);

        BoxLayout layout = new BoxLayout(contentContainer, BoxLayout.PAGE_AXIS);
        contentContainer.setLayout(layout);

        container.setContent(rootContentContainer);
    }

    private ActionButton createToolbarButton(LocalizedString description, char iconId, final Action action) {
        Icon icon = new FontAwesomeIcon(iconId, FONT_SIZE);
        ActionButton button = new ActionButton(icon);
        button.setToolTipText(description.getContents());
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                actionNotifier.fireAction(action);
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

    public void clearAll() {
        for (Map.Entry<String, VmNotePanel> entry : tagToPanel.entrySet()) {
            contentContainer.remove(entry.getValue());
        }
        tagToPanel.clear();
    }

    public void add(VmNoteViewModel model) {
        VmNotePanel widget = new VmNotePanel(model, actionNotifier);
        tagToPanel.put(model.tag, widget);
        contentContainer.add(widget, contentContainerConstraints);
        contentContainer.revalidate();
    }

    public String getContent(String tag) {
        return tagToPanel.get(tag).getContent();
    }

}
