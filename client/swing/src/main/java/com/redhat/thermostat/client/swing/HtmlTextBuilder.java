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

package com.redhat.thermostat.client.swing;

public class HtmlTextBuilder {

    /*
     * The api provided by this class needs to be cleaned up.
     */

    private final StringBuilder text = new StringBuilder();

    public HtmlTextBuilder() {
        // do nothing
    }

    public HtmlTextBuilder(String text) {
        text = escape(text);
        this.text.append(text);
    }

    public HtmlTextBuilder bold(boolean on) {
        if (on) {
            this.text.append("<b>");
        } else {
            this.text.append("</b>");
        }
        return this;
    }

    public HtmlTextBuilder bold(String toBold) {
        text.append("<b>").append(toBold).append("</b>");
        return this;
    }

    public HtmlTextBuilder larger(String toAppend) {
        text.append("<font size='+2'>").append(escape(toAppend)).append("</font>");
        return this;
    }

    public HtmlTextBuilder huge(String toAppend) {
        text.append("<font size='+6'>").append(escape(toAppend)).append("</font>");
        return this;
    }

    @Override
    public String toString() {
        // FIXME
        return null;
    }

    public String toHtml() {
        return "<html>" + text.toString() + "</html>";
    }

    public String toPartialHtml() {
        return text.toString();
    }

    private static String escape(String toEscape) {
        // FIXME implement this
        return toEscape;
    }

    public HtmlTextBuilder append(String toAppend) {
        text.append(escape(toAppend));
        return this;
    }

    public HtmlTextBuilder appendRaw(String toAppend) {
        text.append(toAppend);
        return this;
    }

    public static String boldHtml(String toBold) {
        return new HtmlTextBuilder().bold(toBold).toHtml();
    }

    public HtmlTextBuilder newLine() {
        text.append("<br>");
        return this;
    }

}

