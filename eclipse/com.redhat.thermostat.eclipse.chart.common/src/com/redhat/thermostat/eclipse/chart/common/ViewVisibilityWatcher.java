/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.eclipse.chart.common;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.redhat.thermostat.client.core.views.BasicView.Action;
import com.redhat.thermostat.common.ActionNotifier;

public class ViewVisibilityWatcher {
    
    private ActionNotifier<Action> notifier;
    private boolean visible;

    public ViewVisibilityWatcher(ActionNotifier<Action> notifier) {
        this.notifier = notifier;
        this.visible = false;
    }
    
    public void watch(Composite parent, final String viewID) {
        // Check if the view is currently visible
        IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IViewPart chartView = activePage.findView(viewID);
        if (activePage.isPartVisible(chartView)) {
            visible = true;
            notifier.fireAction(Action.VISIBLE);
        }
        
        // Register listener for visibility updates on the Eclipse view
        final IPartListener2 partListener = new IPartListener2() {
            
            @Override
            public void partVisible(IWorkbenchPartReference partRef) {
                // The workbench fires a visible event when the view first takes
                // focus, even if it was already on top
                if (!visible && viewID.equals(partRef.getId())) {
                    notifier.fireAction(Action.VISIBLE);
                    visible = true;
                }
            }
            
            @Override
            public void partHidden(IWorkbenchPartReference partRef) {
                if (visible && viewID.equals(partRef.getId())) {
                    notifier.fireAction(Action.HIDDEN);
                    visible = false;
                }
            }

            @Override
            public void partOpened(IWorkbenchPartReference partRef) {
            }
            
            @Override
            public void partInputChanged(IWorkbenchPartReference partRef) {
            }
            
            @Override
            public void partDeactivated(IWorkbenchPartReference partRef) {
            }
            
            @Override
            public void partClosed(IWorkbenchPartReference partRef) {
            }
            
            @Override
            public void partBroughtToTop(IWorkbenchPartReference partRef) {
            }
            
            @Override
            public void partActivated(IWorkbenchPartReference partRef) {
            }
        };
        
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().addPartListener(partListener);
        
        parent.addDisposeListener(new DisposeListener() {
            
            @Override
            public void widgetDisposed(DisposeEvent e) {
                PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
                    
                    @Override
                    public void run() {
                        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                        if (window != null) {
                            window.getPartService().removePartListener(partListener);
                        }
                    }
                });
            }
        });
    }

}
