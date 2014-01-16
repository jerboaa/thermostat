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

package com.redhat.thermostat.client.swing.components;

import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

/**
 * Apply Gaussian Blur on images.
 * 
 * <br /><br />
 *
 * The formula for Gaussian blur is:
 *
 * <pre>
 * 
 * G(x, y) = ({@link Math#E} ^ (-(x^2 + y^2) / (2 * SIGMA^2))) / sqrt(2 * {@link Math#PI} * SIGMA^2))
 * 
 * </pre>
 *
 * Where {@code SIGMA} is the standard deviation, {@code x} and {@code y}
 * represent the distance from the horizontal and vertical axis respectively.
 * 
 * In this implementation {@code SIGMA} is always 1/3 of the radius of the
 * applied filter.
 * 
 * <br /><br />
 *
 * Since the Gaussian filter is separable, meaning that can be applied to a
 * two-dimensional image as two independent one-dimensional calculations,
 * so this filter will first create a {@link Kernel} for the horizontal
 * direction and then a second {@link Kernel} for the vertical one.
 */
public class GaussianBlur {
        
    static ConvolveOp[] createFilters(int radius) {
        ConvolveOp[] filters = new ConvolveOp[2];
        
        double sigma = radius / 3.0;
        double sigmaSquareDivisor = 2.0 * Math.pow(sigma, 2);
        
        double sqrtDivisor = Math.sqrt(sigmaSquareDivisor * Math.PI);
        
        float total = 0f;
        float [] matrix = new float[radius * 2];
        for (int i = -radius; i < radius; i++) {
            
            double distance = -(i * i);
            double midpoint = Math.exp(distance / sigmaSquareDivisor) / sqrtDivisor;
            
            matrix[i + radius] = (float) midpoint;
            
            // keep this to normalise the matrix to avoid a darkening or
            // brightening of the image
            total += (float) midpoint;
        }
        
        // normalise the matrix now
        for (int i = 0; i < matrix.length; i++) {
            matrix[i] /= total;
        }
        
        Kernel horizontalKernel = new Kernel(matrix.length, 1, matrix);
        Kernel verticalKernel = new Kernel(1, matrix.length, matrix);
        
        filters[0] = new ConvolveOp(horizontalKernel, ConvolveOp.EDGE_NO_OP, null);  
        filters[1] = new ConvolveOp(verticalKernel, ConvolveOp.EDGE_NO_OP, null);  

        return filters;
    }

    public static BufferedImage applyFilter(int radius, BufferedImage src) {
        ConvolveOp[] filters = GaussianBlur.createFilters(radius);
               
        src = filters[0].filter(src, null);
        src = filters[1].filter(src, null);

        return src;
    }
}

