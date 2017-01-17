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

package com.redhat.thermostat.ui.swing.components.graph;

import com.redhat.thermostat.client.swing.components.ThermostatScrollPane;
import com.redhat.thermostat.platform.swing.components.ContentPane;
import com.redhat.thermostat.ui.swing.model.Trace;
import com.redhat.thermostat.ui.swing.model.graph.GraphModel;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.util.Random;

/**
 */
public class GraphContainer_DynamicTest {
    private static void populate(GraphModel model) {
        Trace trace0 = new Trace("trace0");
        trace0.add("A");
        for (int i = 0; i  < 5; i++) {
            model.addTrace(trace0);
        }

        Trace trace1 = new Trace("trace0");
        trace1.add("A").add("B").add("C").add("D");
        for (int i = 0; i  < 15; i++) {
            model.addTrace(trace1);
        }

        Trace trace2 = new Trace("trace0");
        trace2.add("A").add("B").add("E");
        for (int i = 0; i  < 10; i++) {
            model.addTrace(trace2);
        }

        Trace trace3 = new Trace("trace0");
        trace3.add("A").add("B");
        for (int i = 0; i  < 5; i++) {
            model.addTrace(trace3);
        }

        Trace trace4 = new Trace("trace1");
        trace4.add("F").add("C");
        for (int i = 0; i  < 5; i++) {
            model.addTrace(trace4);
        }

        Trace trace5 = new Trace("trace1");
        trace5.add("F");
        for (int i = 0; i  < 5; i++) {
            model.addTrace(trace5);
        }
    }

    private static void populate2(GraphModel model) {
        Trace trace0 = new Trace("trace0");
        trace0.add("A");
        for (int i = 0; i  < 5; i++) {
            model.addTrace(trace0);
        }

        Trace trace1 = new Trace("trace0");
        trace1.add("A").add("C").add("C").add("C").add("C").add("F").add("G").add("H").
        add("C").add("C").add("C").add("C").add("F").add("G").add("H").
        add("C").add("C").add("C").add("C").add("F").add("G").add("H").
        add("I").add("L").add("M").add("N").add("O").add("P").add("Q");
        for (int i = 0; i  < 15; i++) {
            model.addTrace(trace1);
        }

        Trace trace2 = new Trace("trace1");
        trace2.add("A").add("F");
        for (int i = 0; i  < 5; i++) {
            model.addTrace(trace2);
        }
    }

    private static void populate3(GraphModel model, Random random) {
        int letter = random.nextInt(5);

        Trace trace0 = new Trace("trace" + letter);
        trace0.add("" + letter);
        for (int i = 0; i  < 500; i++) {
            model.addTrace(trace0);
        }

        if (random.nextBoolean()) {
            trace0.add("" + random.nextInt(50));
            for (int i = 0; i  < 300; i++) {
                model.addTrace(trace0);
            }
        }
    }

    private static class ThreadCreator implements Runnable {
        private GraphModel model;
        private Random random;

        public ThreadCreator(GraphModel model) {
            this.model = model;
            this.random = new Random(0l);
        }

        @Override
        public void run() {
            int i = 0;
            while (true) {
                if (random.nextBoolean()) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            populate(model);
                        }
                    });
                } else {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            populate2(model);
                        }
                    });
                }

                i++;

                if (i == 100) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            populate3(model, random);
                        }
                    });
                    i = 0;
                }

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        model.rebuild();
                    }
                });

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame("Thermostat Flame Graph Test");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setMinimumSize(new Dimension(800, 600));

                ContentPane pane = new ContentPane();
                frame.setContentPane(pane);

                GraphModel model = new GraphModel("test app");

                ThreadCreator creator = new ThreadCreator(model);
                Thread thread = new Thread(creator);
                thread.setDaemon(true);
                thread.start();

                GraphContainer graphContainer = new GraphContainer(model);
                ThermostatScrollPane scrollPane = new ThermostatScrollPane(graphContainer);

                pane.add(scrollPane);

                frame.setVisible(true);
            }
        });
    }
}
