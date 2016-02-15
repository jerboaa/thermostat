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

package com.redhat.thermostat.client.swing.internal.accordion;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.redhat.thermostat.client.swing.GraphicsUtils;
import com.redhat.thermostat.client.swing.components.EmptyIcon;
import com.redhat.thermostat.client.swing.components.FontAwesomeIcon;
import com.redhat.thermostat.client.swing.components.Icon;
import com.redhat.thermostat.client.swing.components.ShadowLabel;
import com.redhat.thermostat.client.swing.components.VerticalLayout;
import com.redhat.thermostat.client.ui.Palette;

/**
 * The TitledPane is responsible to render the header component of a new
 * entry in an {@link Accordion}. By default the header is rendered as a
 * {@link ShadowLabel} widget, but can be fine tuned passing appropriate
 * instances of {@link TitledPanePainter} and {@link AccordionComponent}
 * to the constructor.
 */
@SuppressWarnings("serial")
public class TitledPane extends JPanel implements AccordionComponent {
    
    // TODO: move in UIDefault or something class
    protected static final int EXPANDER_ICON_SIZE = 12;
    
    public static final Color SELECTED_FG = Palette.EARL_GRAY.getColor();
    public static final Color UNSELECTED_FG = Palette.DARK_GRAY.getColor();
    
    private Color selectedColor;
    private Color unselectedColor;

    public static final String EXPANDED_PROPERTY = "EXPANDED_PROPERTY";

    private boolean expanded;
    
    private boolean selected;
    
    private Icon expandedIcon;
    private Icon expandedSelectedIcon;
    
    private Icon collapsedIcon;
    private Icon collapsedIconSelectedIcon;
    
    private Icon emptyIcon;
    
    private JLabel iconLabel;    

    private JPanel titlePane;
    
    private JComponent content;

    private TitledPanePainter backgroundPainter;
    
    private class DefaultTitleComponent implements AccordionComponent {
        private ShadowLabel titleLabel;
        public DefaultTitleComponent(String title) {
            titleLabel = new ShadowLabel();
            titleLabel.setText(title);
            titleLabel.setName(title + "_label");
            titleLabel.setForeground(unselectedColor);
        }
        
        @Override
        public Component getUiComponent() {
            return titleLabel;
        }

        @Override
        public void setSelected(boolean selected) {
            if (selected) {
                titleLabel.setForeground(selectedColor);
            } else {
                titleLabel.setForeground(unselectedColor);
            }
        }

        @Override
        public boolean isSelected() {
            return TitledPane.this.isSelected();
        }
    }
    
    private AccordionComponent titleComponent;
    
    public TitledPane(String title, TitledPanePainter backgroundPainter) {
        this(title, backgroundPainter, null);
    }
    
    public TitledPane(String title, TitledPanePainter backgroundPainter,
                      AccordionComponent titleComponent)
    {
        setLayout(new VerticalLayout());
        setName(title);
        
        titlePane = new JPanel();
        titlePane.setLayout(new BorderLayout());
        
        if (backgroundPainter != null) {
           
            this.backgroundPainter = backgroundPainter;
            
            selectedColor = backgroundPainter.getSelectedForeground();
            unselectedColor = backgroundPainter.getUnselectedForeground();
            titlePane.setOpaque(false);
            
        } else {
            selectedColor = SELECTED_FG;
            unselectedColor = UNSELECTED_FG;
        }

        if (titleComponent == null) {
            titleComponent = new DefaultTitleComponent(title);
        }
        this.titleComponent = titleComponent;

        expanded = false;
        
        int iconSize = EXPANDER_ICON_SIZE;
        
        emptyIcon = new EmptyIcon(iconSize, iconSize);

        // TODO: move in UIDefault, especially the constants
        expandedIcon = new FontAwesomeIcon('\uf107', iconSize, unselectedColor);
        expandedSelectedIcon = new FontAwesomeIcon('\uf107', iconSize, selectedColor);
        
        collapsedIcon = new FontAwesomeIcon('\uf105', iconSize, unselectedColor);
        collapsedIconSelectedIcon = new FontAwesomeIcon('\uf105', iconSize, selectedColor);

        iconLabel = new JLabel(emptyIcon);
        iconLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        
        iconLabel.setName(title + "_ExpanderIcon");
        iconLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setExpanded(!isExpanded());
            }
        });
        titlePane.add(iconLabel, BorderLayout.WEST);
        titlePane.add(this.titleComponent.getUiComponent(), BorderLayout.CENTER);
        
        add(titlePane);
    }
        
    public TitledPane(String title) {
        this(title, null);
    }

    private void updateState() {
        
        titleComponent.setSelected(isSelected());
        if (isExpanded()) {            
            if (isSelected()) {
                iconLabel.setIcon(expandedSelectedIcon);                
            } else {
                iconLabel.setIcon(expandedIcon);
            }
        } else {
            if (isSelected()) {
                iconLabel.setIcon(collapsedIconSelectedIcon);
            } else {
                iconLabel.setIcon(collapsedIcon);
            }
        }
        
        if (!hasContent()) {
            iconLabel.setIcon(emptyIcon);
        }
        
        repaint();
    }
    
    protected AccordionComponent getTitleComponent() {
        return titleComponent;
    }
    
    public boolean isExpanded() {
        return expanded;
    }
    
    public void setExpanded(boolean expanded) {
        if (hasContent()) {
            boolean oldExpaned = this.expanded;
            this.expanded = expanded;
            updateState();
            if (oldExpaned != expanded) {
                if (expanded) {
                    add(content);
                } else {
                    remove(content);
                }
                revalidate();
                repaint();
                firePropertyChange(EXPANDED_PROPERTY, oldExpaned, expanded);
            }
        }
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
        updateState();
    }

    private boolean hasContent() {
        return content != null && content.getComponentCount() != 0;
    }
    
    @Override
    public boolean isSelected() {
        return selected;
    }

    public JComponent getContent() {
        return content;
    }
    
    private void collapse() {
        remove(content);
        expanded = false;
        revalidate();
        repaint();
    }
    
    public void setContent(JComponent content) {
        JComponent oldContent = this.content;
        this.content = content;
        if (content != null) {
            content.addContainerListener(new ContainerListener() {
                @Override
                public void componentAdded(ContainerEvent e) {
                    updateState();
                }
                
                @Override
                public void componentRemoved(ContainerEvent e) {
                    if (!hasContent()) {
                        collapse();
                    }
                    updateState();
                }
            });
        }
        
        if (isExpanded()) {
            if (oldContent != null) {
                remove(this.content);
            }
            setExpanded(true);
        }
        
        updateState();
    }
    
    @Override
    public Component getUiComponent() {
        return this;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundPainter != null) {
            Graphics2D graphics = GraphicsUtils.getInstance().createAAGraphics(g);
            backgroundPainter.paint(graphics, this, getWidth(), getHeight());
            graphics.dispose();
        }
    }
}

