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

package com.redhat.thermostat.eclipse.internal.preferences;

import java.io.IOException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.redhat.thermostat.client.ui.ClientPreferencesModel;
import com.redhat.thermostat.common.config.ClientPreferences;
import com.redhat.thermostat.eclipse.LoggerFacility;
import com.redhat.thermostat.eclipse.ThermostatConstants;
import com.redhat.thermostat.eclipse.internal.Activator;

/**
 * Main preferences page for the Thermostat Eclipse client.
 *
 */
public class MainPreferencePage extends FieldEditorPreferencePage implements
        IWorkbenchPreferencePage {
    
    private static final int GROUP_SPAN = 2;
    private static final String HTTP_PREFIX = "http";
    private static final String MONGODB_PREFIX = "mongodb";
    
    private StringFieldEditor connectionUrlEditor;
    private StringFieldEditor usernameEditor;
    private StringFieldEditor passwordEditor;
    private BooleanFieldEditor saveEntitlementsEditor;
    private ClientPreferencesModel clientPrefs;
    
    /**
     * Default no-arg constructor.
     */
    public MainPreferencePage() {
        super(GRID);
        // TODO: Externalize string.
        setDescription("Thermostat Client Preferences");
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
    }
    
    /**
     * Validate fields for sane values.
     */
    @Override
    public void checkState() {
        super.checkState();
        // connection URL has to be http(s) or mongodb
        if (connectionUrlEditor.getStringValue() != null
                && !(connectionUrlEditor.getStringValue().startsWith(HTTP_PREFIX)
                        || connectionUrlEditor.getStringValue().startsWith(MONGODB_PREFIX))) {
            setErrorMessage("Connection URL must start with either 'http' or 'mongodb'");
            setValid(false);
        } else {
            // erase error message and mark things valid
            setErrorMessage(null);
            setValid(true);
        }
    }
    
    @Override
    public boolean performOk() {
        clientPrefs.setConnectionUrl(connectionUrlEditor.getStringValue());
        clientPrefs.setSaveEntitlements(saveEntitlementsEditor.getBooleanValue());
        if (saveEntitlementsEditor.getBooleanValue()) {
            // FIXME Eclipse "Text" doesn't have a widget to return a char[] password, yet.
            // https://bugs.eclipse.org/bugs/show_bug.cgi?id=297412
            clientPrefs.setCredentials(usernameEditor.getStringValue(), passwordEditor.getTextControl(getFieldEditorParent()).getText().toCharArray());
            try {
                clientPrefs.flush();
            } catch (IOException e) {
                LoggerFacility.getInstance().log(IStatus.ERROR, "Failed to save preferences", e);
            }
        }
        return true;
    }
    
    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    @Override
    public void init(IWorkbench arg0) {
        // nothing
    }
    
    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent event) {
        /*
         * Validate input on change events.
         */
        if (event.getProperty().equals(FieldEditor.VALUE)) {
            checkState();
        }
    }

    @Override
    protected void createFieldEditors() {
        Composite composite = getFieldEditorParent();
        
        // General prefs
        Group generalGroup = new Group(composite, SWT.SHADOW_ETCHED_IN);
        generalGroup.setText("General");

        GridDataFactory.fillDefaults().grab(true, false).span(GROUP_SPAN, 1)
                .applyTo(generalGroup);
        connectionUrlEditor = new StringFieldEditor(
                ThermostatConstants.CONNECTION_URL_PREF_NAME,
                "Connection URL",
                generalGroup);
        usernameEditor = new StringFieldEditor(
                ThermostatConstants.USERNAME_PREF_NAME,
                "Username",
                generalGroup);
        passwordEditor = new StringFieldEditor(ThermostatConstants.PASSWORD_PREF_NAME,
                "Password", generalGroup);
        passwordEditor.getTextControl(generalGroup).setEchoChar('*');
        saveEntitlementsEditor = new BooleanFieldEditor(
                ThermostatConstants.SAVE_ENTITLEMENTS_PREF_NAME,
                "Save Entitlements", generalGroup);
        // register change listener
        connectionUrlEditor.setPropertyChangeListener(this);
        addField(connectionUrlEditor);
        addField(usernameEditor);
        addField(passwordEditor);
        addField(saveEntitlementsEditor);
        updateMargins(generalGroup);
        this.clientPrefs = new ClientPreferencesModel(Activator.getDefault().getKeyring(), new ClientPreferences(Activator.getDefault().getCommonPaths()));
        synchronizeValues();
    }
    
    private void updateMargins(Group group) {
        // make sure there is some room between the group border
        // and the controls in the group
        GridLayout layout = (GridLayout) group.getLayout();
        layout.marginWidth = 5;
        layout.marginHeight = 5;
    }
    
    private void synchronizeValues() {
        IEclipsePreferences node = DefaultScope.INSTANCE.getNode(Activator.PLUGIN_ID);
        if (clientPrefs.getSaveEntitlements()) { 
            node.put(ThermostatConstants.USERNAME_PREF_NAME, clientPrefs.getUserName());
            // FIXME Need Eclipse prefs and widgets that supports char[]
            String passString = new String(clientPrefs.getPassword());
            node.put(ThermostatConstants.PASSWORD_PREF_NAME, passString);
            passwordEditor.setStringValue(passString);
            usernameEditor.setStringValue(clientPrefs.getUserName());
        } else {
            try {
                node.clear();
            } catch (org.osgi.service.prefs.BackingStoreException e) {
                LoggerFacility.getInstance().log(IStatus.ERROR, "Failed to clear preferences", e);
            }
        }
        node.put(ThermostatConstants.CONNECTION_URL_PREF_NAME,
                clientPrefs.getConnectionUrl());
        connectionUrlEditor.setStringValue(clientPrefs.getConnectionUrl());
        node.putBoolean(ThermostatConstants.SAVE_ENTITLEMENTS_PREF_NAME, clientPrefs.getSaveEntitlements());
    }

}
