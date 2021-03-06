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

package com.redhat.thermostat.client.ui;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.client.core.views.ClientConfigurationView;
import com.redhat.thermostat.client.core.views.ClientConfigurationView.Action;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.StorageCredentials;
import com.redhat.thermostat.utils.keyring.KeyringException;

public class ClientConfigurationController implements ActionListener<Action> {

    private static final Logger logger = LoggingUtils.getLogger(ClientConfigurationController.class);

    private final ClientConfigurationView view;
    private final ClientPreferencesModel model;
    private final ClientConfigReconnector reconnector;

    public ClientConfigurationController(ClientPreferencesModel model, ClientConfigurationView view) {
        this(model, view, null);
    }

    public ClientConfigurationController(ClientPreferencesModel model, ClientConfigurationView view, ClientConfigReconnector reconnector) {
        this.model = model;
        this.view = view;
        this.reconnector = reconnector;
        view.addListener(this);
    }

    public void showDialog() {
        updateViewFromModel();
        view.showDialog();
    }

    private void updateViewFromModel() {
        view.setSaveEntitlements(model.getSaveEntitlements());
        view.setConnectionUrl(model.getConnectionUrl());
        
        view.setPassword(model.getPassword());
        view.setUserName(model.getUserName());
    }

    private void updateModelFromView() {
        model.setSaveEntitlements(view.getSaveEntitlements());
        model.setConnectionUrl(view.getConnectionUrl());
        
        try {
            model.setCredentials(view.getUserName(), view.getPassword());
            model.flush();
        } catch (IOException|KeyringException e) {
            logger.log(Level.WARNING, "error saving client preferences", e);
        }
    }

    @Override
    public void actionPerformed(ActionEvent<Action> actionEvent) {
        if (actionEvent.getSource() != view) {
            return;
        }
        
        switch (actionEvent.getActionId()) {
            case CLOSE_ACCEPT:
                updateModelFromView();
                view.hideDialog();
                if (reconnector != null) {
                    reconnector.reconnect(model.getPreferences(), new PreferencesModelStorageCredentials(model));
                }
                break;
            case CLOSE_CANCEL:
                view.hideDialog();
                if (reconnector != null) {
                    reconnector.abort();
                }
                break;
        }

    }

    class PreferencesModelStorageCredentials implements StorageCredentials {

        private ClientPreferencesModel model;

        PreferencesModelStorageCredentials(ClientPreferencesModel model) {
            this.model = model;
        }

        public String getUsername() {
            return model.getUserName();
        }

        @Override
        public char[] getPassword() {
            return model.getPassword();
        }
        
    }
}

