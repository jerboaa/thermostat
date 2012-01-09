package com.redhat.thermostat.client.ui;

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

    private static String escape(String text) {
        // FIXME implement this
        return text;
    }

    public HtmlTextBuilder append(String value) {
        text.append(value);
        return this;
    }

    public static String boldHtml(String toBold) {
        return new HtmlTextBuilder().bold(toBold).toHtml();
    }

}
