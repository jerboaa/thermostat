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

package com.redhat.thermostat.client.swing.components;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * Creates an {@link Icon} from a given FontAwesome unicode identifier.
 * 
 * @see http://fortawesome.github.io/Font-Awesome/cheatsheet/
 */
@SuppressWarnings("serial")
public class FontAwesomeIcon extends Icon {

    private static final String AWESOME_SET = "fontawesome-webfont.ttf";
    
    private int size;
    private BufferedImage buffer;
    
    private char iconID;
    private static final Font awesome;
    
    private Font font;
    
    private Paint color;
    
    static {
        ClassLoader loader = FontAwesomeIcon.class.getClassLoader();
        try (InputStream stream = loader.getResourceAsStream(AWESOME_SET)) {
            awesome = Font.createFont(Font.TRUETYPE_FONT, stream);

        } catch (FontFormatException | IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Creates a new {@link FontAwesomeIcon} painted in {@link Color#BLACK}.
     * 
     * <br /><br />
     * 
     * Please, refer to:
     * <a href='http://fortawesome.github.io/Font-Awesome/cheatsheet/'>
     * Font-Awesome Website</a> for the actual
     * values accepted as {@code iconID}.
     * 
     * @see {@link http://fortawesome.github.io/Font-Awesome/cheatsheet/}
     */
    public FontAwesomeIcon(char iconID, int size) {
        this(iconID, size, Color.BLACK);
    }

    /**
     * Creates a new {@link FontAwesomeIcon} painted in the given {@link Paint}.
     * 
     * <br /><br />
     * 
     * Please, refer to:
     * <a href='http://fortawesome.github.io/Font-Awesome/cheatsheet/'>
     * Font-Awesome Website</a> for the actual
     * values accepted as {@code iconID}.
     * 
     * @see {@link http://fortawesome.github.io/Font-Awesome/cheatsheet/}
     */
    public FontAwesomeIcon(char iconID, int size, Paint color) {
        this.iconID = iconID;
        this.size = size;
        font = awesome.deriveFont(Font.PLAIN, size);
        this.color = color;
    }
    
    @Override
    public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
        
        if (buffer == null) {
            buffer = new BufferedImage(getIconWidth(), getIconHeight(),
                                       BufferedImage.TYPE_INT_ARGB);
                        
            Graphics2D graphics = (Graphics2D) buffer.getGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                      RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                                      RenderingHints.VALUE_RENDER_QUALITY);

            graphics.setFont(font);
            graphics.setPaint(color);

            String icon = String.valueOf(iconID);

            FontRenderContext fontRenderContext = graphics.getFontRenderContext();
            TextLayout layout = new TextLayout(icon, font, fontRenderContext);

            FontMetrics metrics = graphics.getFontMetrics(font);

            int stringX = (getIconWidth() - metrics.stringWidth(icon))/2;
            int stringY = (metrics.getAscent() + (getIconHeight() - (metrics.getAscent() + metrics.getDescent())) / 2);

            graphics.drawString(String.valueOf(iconID), stringX, stringY);

            graphics.setColor(Color.RED);
            graphics.dispose();
        }
        
        g.drawImage(buffer, x, y, null);
    }

    @Override
    public Image getImage() {
        return super.getImage();
    }
    
    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public int getIconWidth() {
        return size;
    }

}

