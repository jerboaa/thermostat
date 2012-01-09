package com.redhat.thermostat.client.ui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JPanel;

public class SimpleTable {

    public static class Section {
        private final String sectionName;
        private final List<TableEntry> tableEntries = new ArrayList<TableEntry>();

        public Section(String name) {
            this.sectionName = name;
        }

        public String getText() {
            return sectionName;
        }

        public void add(TableEntry entry) {
            tableEntries.add(entry);
        }

        public void add(Key key, List<Value> values) {
            tableEntries.add(new TableEntry(key, values));
        }

        public void add(Key key, Value value) {
            tableEntries.add(new TableEntry(key, value));
        }

        public TableEntry[] getEntries() {
            return tableEntries.toArray(new TableEntry[0]);
        }
    }

    public static class TableEntry {
        private final Key key;
        private final List<Value> values;

        public TableEntry(String key, String value) {
            this(new Key(key), new Value(value));
        }

        public TableEntry(Key key, Value value) {
            this.key = key;
            this.values = new ArrayList<Value>();
            this.values.add(value);
        }

        public TableEntry(Key key, List<Value> values) {
            this.key = key;
            this.values = new ArrayList<Value>(values);
        }

        public Key getKey() {
            return key;
        }

        public Value[] getValues() {
            return values.toArray(new Value[0]);
        }

    }

    public static class Key {
        private final String text;

        public Key(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

    public static class Value {
        private final String text;

        public Value(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

    public static JPanel createTable(List<Section> sections) {
        final int SECTION_TOP_GAP = 10;
        final int ROW_VERTICAL_GAP = 0;
        final int ROW_HORIZONTAL_GAP = 10;

        Insets sectionHeaderInsets = new Insets(SECTION_TOP_GAP, 0, 0, 0);
        Insets rowInsets = new Insets(ROW_VERTICAL_GAP, ROW_HORIZONTAL_GAP, ROW_VERTICAL_GAP, ROW_HORIZONTAL_GAP);

        JPanel container = new JPanel();
        container.setLayout(new GridBagLayout());

        GridBagConstraints keyConstraints = new GridBagConstraints();
        GridBagConstraints valueConstraints = new GridBagConstraints();
        GridBagConstraints sectionHeaderConstraints = new GridBagConstraints();

        keyConstraints.insets = valueConstraints.insets = rowInsets;
        keyConstraints.gridy = valueConstraints.gridy = 0;
        keyConstraints.gridx = 0;
        valueConstraints.gridx = 1;
        keyConstraints.fill = valueConstraints.fill = GridBagConstraints.HORIZONTAL;

        sectionHeaderConstraints.gridx = 0;
        sectionHeaderConstraints.gridwidth = GridBagConstraints.REMAINDER;
        sectionHeaderConstraints.fill = GridBagConstraints.HORIZONTAL;
        sectionHeaderConstraints.insets = sectionHeaderInsets;

        for (Section section : sections) {
            sectionHeaderConstraints.gridy = keyConstraints.gridy = ++valueConstraints.gridy;
            container.add(Components.header(section.getText()), sectionHeaderConstraints);
            for (TableEntry tableEntry : section.getEntries()) {
                keyConstraints.gridy = ++valueConstraints.gridy;
                container.add(Components.label(tableEntry.getKey().getText()), keyConstraints);

                for (Value value : tableEntry.getValues()) {
                    container.add(Components.value(value.getText()), valueConstraints);
                    keyConstraints.gridy = ++valueConstraints.gridy;
                }
            }
        }

        GridBagConstraints glueConstraints = new GridBagConstraints();
        glueConstraints.gridy = keyConstraints.gridy + 1;
        glueConstraints.gridx = 0;
        glueConstraints.weightx = 1;
        glueConstraints.weighty = 1;
        glueConstraints.fill = GridBagConstraints.BOTH;
        glueConstraints.gridheight = GridBagConstraints.REMAINDER;
        glueConstraints.gridwidth = GridBagConstraints.REMAINDER;
        Component filler = Box.createGlue();
        container.add(filler, glueConstraints);

        return container;
    }

}
