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

package com.redhat.thermostat.platform.swing;

import com.redhat.thermostat.platform.annotations.PlatformService;
import com.redhat.thermostat.platform.mvc.View;
import com.redhat.thermostat.platform.swing.components.ContentPane;

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

@PlatformService(service = {
        WorkbenchSwingView.class,
        View.class,
})
public class WorkbenchSwingView extends SwingView {

    private JFrame frame;
    private WindowAdapter adapter;

    @Override
    protected void postCreate(ContentPane contentPane) {
        frame = createFrame();
        adapter = new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                showingProperty().setValue(false);
            }

            @Override
            public void windowOpened(WindowEvent e) {
                showingProperty().setValue(true);
            }
        };

        contentPane.setName("Thermostat Platform Swing Workbench");
    }

    public JFrame getFrame() {
        return frame;
    }

    /**
     * Returns the frame used as main frame for the whole application.
     */
    protected JFrame createFrame() {
        return new JFrame();
    }

    /**
     * Called before window listeners and content pane are attached to
     * this frame. The frame passed to this method is the same instance
     * returned by {@link #createFrame}.
     *
     * <br /><br />
     *
     * The default implementation sets a minimum size of the frame.
     */
    protected void preInitFrame(JFrame frame) {
        frame.setMinimumSize(new Dimension(800, 800));
    }

    @Override
    protected void postDestroy() {
        frame.dispose();
    }

    @Override
    public void stop() {
        frame.setVisible(false);
    }

    @Override
    protected void init() {

        preInitFrame(frame);

        frame.addWindowListener(adapter);
        frame.setContentPane(contentPane);

        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    }

    @Override
    public void start() {
        frame.setVisible(true);
    }
}
