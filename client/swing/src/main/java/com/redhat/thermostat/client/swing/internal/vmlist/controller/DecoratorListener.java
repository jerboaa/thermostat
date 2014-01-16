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

package com.redhat.thermostat.client.swing.internal.vmlist.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Ordered;
import com.redhat.thermostat.common.OrderedComparator;
import com.redhat.thermostat.common.ThermostatExtensionRegistry;
import com.redhat.thermostat.common.ThermostatExtensionRegistry.Action;
import com.redhat.thermostat.common.utils.LoggingUtils;

/**
 *
 */
public class DecoratorListener<D extends Ordered> extends DecoratorNotifier
    implements ActionListener<ThermostatExtensionRegistry.Action>
{
    private static final Logger logger =
            LoggingUtils.getLogger(DecoratorListener.class);
    
    private CopyOnWriteArrayList<D> decorators;
    private Class<D> type;
    
    public DecoratorListener(Class<D> type) {
        decorators = new CopyOnWriteArrayList<>();
        this.type = type;
    }
    
    @Override
    public void actionPerformed(ActionEvent<Action> actionEvent) {
        Object payload = actionEvent.getPayload();
        if (!type.isAssignableFrom(payload.getClass())) {
            // be permissive, but log this
            logger.log(Level.WARNING, "Dropping unexpected payload type: " +
                                      payload.getClass().getName());
        } else {
            
            @SuppressWarnings("unchecked")
            D labelDecorator = (D) payload;
            decorators.add(labelDecorator);
            
            switch (actionEvent.getActionId()) {
            case SERVICE_ADDED:
                fireDecorationAdded();
                break;
                
            case SERVICE_REMOVED:
                fireDecorationRemoved();
                break;
                
            default:
                fireDecorationChanged();
                break;
            }
        }
    }

    public List<D> getDecorators() {
        
        List<D> result = new ArrayList<>(decorators);
        Collections.sort(result, new OrderedComparator<D>());
        return result;
    }
}

