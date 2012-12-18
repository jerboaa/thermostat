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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Thermostat; see the file COPYING. If not see
 * <http://www.gnu.org/licenses/>.
 *
 * Linking this code with other modules is making a combined work
 * based on this code. Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this code give
 * you permission to link this code with independent modules to
 * produce an executable, regardless of the license terms of these
 * independent modules, and to copy and distribute the resulting
 * executable under terms of your choice, provided that you also
 * meet, for each linked independent module, the terms and conditions
 * of the license of that module. An independent module is a module
 * which is not derived from or based on this code. If you modify
 * this code, you may extend this exception to your version of the
 * library, but you are not obligated to do so. If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.redhat.thermostat.client.swing;

import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.MenuElement;

import com.redhat.thermostat.client.osgi.service.MenuAction;
import com.redhat.thermostat.client.swing.components.EdtHelper;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.common.utils.StringUtils;

/**
 * Helps adding or removing {@link MenuAction} from {@link JMenuBar}s.
 * <p>
 * This automatically handles creation and removal of submenus as appropriate.
 */
public class MenuHelper {

    private static final Logger logger = LoggingUtils.getLogger(MenuHelper.class);

    private final JMenuBar menuBar;

    public MenuHelper(JMenuBar menuBar) {
        this.menuBar = menuBar;
    }

    /**
     * Add a menu item as specified though the {@link MenuAction} argument.
     */
    public void addMenuAction(final MenuAction action) {
        try {
            new EdtHelper().callAndWait(new Runnable() {
                @Override
                public void run() {
                    String[] path = action.getPath();
                    Menu parent = findMenuParent(menuBar, path, true);
                    JMenuItem menu = null;
                    switch (action.getType()) {
                    case RADIO:
                        menu = new JRadioButtonMenuItem();
                        break;
                    case CHECK:
                        menu = new JCheckBoxMenuItem();
                        break;

                    case STANDARD:
                    default:
                        menu = new JMenuItem();
                        break;
                    }

                    menu.setText(action.getName());
                    menu.addActionListener(new java.awt.event.ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            action.execute();
                        }
                    });
                    parent.add(new Menu(menu));

                    menuBar.revalidate();
                }
            });
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) cause;
            }
            throw new RuntimeException(cause);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ie);
        }

    }

    /**
     * Remove a existing menu item as specified though the {@link MenuAction}
     * argument.
     *
     * @throws IllegalArgumentException if the path specified in
     * {@link MenuAction#getPath()} can not be found
     */
    public void removeMenuAction(final MenuAction action) {
        try {
            new EdtHelper().callAndWait(new Runnable() {
                @Override
                public void run() {
                    String[] path = action.getPath();
                    Menu parent = findMenuParent(menuBar, path, false);
                    parent.remove(path[path.length - 1]);
                    menuBar.revalidate();
                }
            });
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ie);
        } catch (InvocationTargetException roe) {
            Throwable cause = roe.getCause();
            if (cause instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) cause;
            }
            throw new RuntimeException(cause);
        }

    }

    private static Menu findMenuParent(JMenuBar menuBar, String[] path, boolean createIfNotFound) {
        Menu parent = null;
        int mainMenuCount = menuBar.getMenuCount();
        for (int i = 0; i < mainMenuCount; i++) {
            JMenu menu = menuBar.getMenu(i);
            if (menu.getText().equals(path[0])) {
                parent = new Menu(menuBar.getMenu(i));
                break;
            }
        }
        if (parent == null) {
            if (createIfNotFound) {
                JMenu delegate = new JMenu(path[0]);
                parent = new Menu(delegate);
                menuBar.add(delegate);
            } else {
                throw new IllegalArgumentException("top-level " + path[0] + " not found (using path" + Arrays.toString(path) + ")");
            }
        }

        for (int i = 1; i < path.length - 1; i++) {
            Menu[] children = parent.children();
            boolean found = false;
            for (int j = 0; j < children.length; j++) {
                Menu menu = children[j];
                if (menu.getText().equals(path[i])) {
                    parent = menu;
                    found = true;
                }
            }
            if (!found) {
                if (createIfNotFound) {
                    Menu newMenu = new Menu(new JMenu(path[i]));
                    parent.add(newMenu);
                    parent = newMenu;
                } else {
                    throw new IllegalArgumentException("path not found");
                }
            }
        }

        return parent;
    }

    private static String getText(MenuElement element) {
        if (element instanceof JMenuItem) {
            return ((JMenuItem) element).getText();
        }
        return "";
    }

    @SuppressWarnings("unused") // this method is for debugging only
    private static void printMenu(MenuElement parent, int nestingLevel) {
        System.out.println(StringUtils.repeat(" ", nestingLevel * 2) + getText(parent) + " [" + parent.getClass() + "]");
        for (MenuElement element : parent.getSubElements()) {
            printMenu(element, nestingLevel + 1);
        }
    }

    /**
     * The swing menu hierarchy makes uniform operations very difficult. This
     * is a hack around that.
     */
    private static final class Menu {
        private Object swingDelegate;

        public Menu() { /* no op */}

        public Menu(JMenuItem actual) {
            this.swingDelegate = actual;
        }

        public String getText() {
            if (swingDelegate instanceof JMenuItem) {
                return ((JMenuItem) swingDelegate).getText();
            }
            return null;
        }

        public Menu[] children() {
            if (swingDelegate instanceof MenuElement) {
                MenuElement[] actualChildren = ((MenuElement) swingDelegate).getSubElements();
                if (actualChildren.length == 1 && actualChildren[0] instanceof JPopupMenu) {
                    actualChildren = actualChildren[0].getSubElements();
                }

                Menu[] children = new Menu[actualChildren.length];
                for (int i = 0; i < children.length; i++) {
                    children[i] = new Menu((JMenuItem) actualChildren[i]);
                }
                return children;
            }
            return new Menu[0];
        }

        public void add(Menu menu) {
            if (swingDelegate instanceof JPopupMenu) {
                ((JPopupMenu) swingDelegate).add((JMenuItem) menu.swingDelegate);
            } else if (swingDelegate instanceof JMenu) {
                ((JMenu) swingDelegate).add((JMenuItem) menu.swingDelegate);
            } else {
                logger.warning("Unable to add menu. Menu is of unrecognized type: " + menu.swingDelegate);
            }
        }

        public void remove(String string) {
            JPopupMenu removeParent = null;

            if (swingDelegate instanceof JMenu) {
                JMenu parent = (JMenu) swingDelegate;
                MenuElement[] actualChildren = parent.getSubElements();
                if (actualChildren.length == 1 && actualChildren[0] instanceof JPopupMenu) {
                    removeParent = (JPopupMenu) actualChildren[0];
                }
            }

            if (removeParent == null) {
                if (swingDelegate instanceof JPopupMenu) {
                    removeParent = (JPopupMenu) swingDelegate;
                } else {
                    logger.warning("BUG: problem while removing menu. delegate is not a JPopupMenu, cant remove");
                    return;
                }
            }

            JPopupMenu parent = removeParent;
            for (MenuElement element : parent.getSubElements()) {
                if (((JMenuItem) element).getText().equals(string)) {
                    parent.remove((JMenuItem) element);
                }
            }
        }

        @Override
        public String toString() {
            return "Menu [" + swingDelegate.toString() + "]";
        }
    }

}
