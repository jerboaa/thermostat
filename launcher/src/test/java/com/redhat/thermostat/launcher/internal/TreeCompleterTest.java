/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.launcher.internal;

import static com.redhat.thermostat.launcher.internal.TreeCompleter.createStringNode;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.redhat.thermostat.common.cli.CommandException;
import jline.console.completer.FileNameCompleter;
import org.junit.Before;
import org.junit.Test;

public class TreeCompleterTest {

    private File testDir;
    private TreeCompleter tree;

    @Before
    public void setUp() throws IOException {
        tree = new TreeCompleter();

        testDir = new File(System.getProperty("java.io.tmpdir") + File.separator + "treecompleter");
        testDir.deleteOnExit();
        testDir.mkdirs();
        setupTreeCompleter();
    }

    @Test
    public void testInlineCompletion() throws IOException, CommandException {
        List<String> candidates = completeBuffer("other");

        assertTrue(candidates.contains("otherCommand"));
    }

    @Test
    public void testBaseIndexCompletion() throws IOException, CommandException {
        List<String> candidates = completeBuffer("");

        assertTrue(candidates.contains("otherCommand"));
        assertTrue(candidates.contains("command1"));
        assertTrue(candidates.contains("command2"));
        assertTrue(candidates.contains("anotherCommand"));
    }

    @Test
    public void testVeryLongCompletion() throws IOException, CommandException {
        String[] longChainOfWords = { "otherCommand", "create", "a", "long", "chain", "of", "tab", "completing", "words"};
        List<String> chain = new ArrayList<String>();
        Collections.addAll(chain, longChainOfWords);
        String firstPart = "";
        for (String word : chain) {
            firstPart += word + " ";
            List<String> tabOutput = completeBuffer(firstPart + " ");

            if (chain.indexOf(word) == 0) {
                assertTrue(tabOutput.contains("list"));
                assertTrue(tabOutput.contains("create"));
                assertTrue(tabOutput.contains("start"));
            } else if (chain.indexOf(word) == chain.size() - 1) {
                assertTrue(tabOutput.contains("otherCommand"));
                assertTrue(tabOutput.contains("anotherCommand"));
                assertTrue(tabOutput.contains("command1"));
                assertTrue(tabOutput.contains("command2"));
            } else {
                assertTrue(tabOutput.contains(chain.get(chain.indexOf(word) + 1)));
            }

        }
    }

    @Test
    public void testVeryLongCompletionRestartedTwice() throws IOException, CommandException {
        String[] longChainOfWords = { "otherCommand", "create", "a", "long", "chain", "of", "tab", "completing", "words"};
        List<String> chain = new ArrayList<String>();
        Collections.addAll(chain, longChainOfWords);
        Collections.addAll(chain, longChainOfWords);
        Collections.addAll(chain, longChainOfWords);
        String firstPart = "";
        for (String word : chain) {
            firstPart += word + " ";
            List<String> tabOutput = completeBuffer(firstPart + " ");

            if (chain.indexOf(word) == 0) {
                assertTrue(tabOutput.contains("list"));
                assertTrue(tabOutput.contains("create"));
                assertTrue(tabOutput.contains("start"));
            } else if (chain.indexOf(word) == chain.size() - 1) {
                assertTrue(tabOutput.contains("otherCommand"));
                assertTrue(tabOutput.contains("anotherCommand"));
                assertTrue(tabOutput.contains("command1"));
                assertTrue(tabOutput.contains("command2"));
            } else {
                assertTrue(tabOutput.contains(chain.get(chain.indexOf(word) + 1)));
            }

        }
    }

    @Test
    public void testRestartIndexOtherThanZero() throws CommandException {
        String[] input = { "command1", "list", "nothing"};
        List<String> chain = new ArrayList<String>();
        Collections.addAll(chain, input);
        String firstPart = "";
        for (String word : chain) {
            firstPart += word + " ";
            List<String> tabOutput = completeBuffer(firstPart + " ");

            if (chain.indexOf(word) == 0) {
                assertTrue(tabOutput.contains("list"));
                assertTrue(tabOutput.contains("create"));
                assertTrue(tabOutput.contains("delete"));
            } else if (chain.indexOf(word) == chain.size() - 1) {
                assertTrue(tabOutput.contains("list"));
                assertTrue(tabOutput.contains("create"));
                assertTrue(tabOutput.contains("delete"));
            } else {
                assertTrue(tabOutput.contains(chain.get(chain.indexOf(word) + 1)));
            }

        }
    }

    @Test
    public void testCompletionOfSameTextInDifferentBranchesReturnsDifferentResults() throws CommandException {
        List<String> firstOutput = completeBuffer("otherCommand list ");
        List<String> secondOutput = completeBuffer("command1 list ");

        assertTrue(firstOutput.contains("parts"));
        assertTrue(firstOutput.contains("assemblies"));
        assertTrue(firstOutput.contains("degreesOfFreedom"));
        assertTrue(firstOutput.contains("bolts"));
        assertTrue(firstOutput.contains("tools"));

        assertFalse(firstOutput.contains("everything"));
        assertFalse(firstOutput.contains("nothing"));
        assertFalse(firstOutput.contains("firstHalf"));
        assertFalse(firstOutput.contains("secondHalf"));

        assertTrue(secondOutput.contains("everything"));
        assertTrue(secondOutput.contains("nothing"));
        assertTrue(secondOutput.contains("firstHalf"));
        assertTrue(secondOutput.contains("secondHalf"));

        assertFalse(secondOutput.contains("parts"));
        assertFalse(secondOutput.contains("assemblies"));
        assertFalse(secondOutput.contains("degreesOfFreedom"));
        assertFalse(secondOutput.contains("bolts"));
        assertFalse(secondOutput.contains("tools"));
    }

    @Test
    public void testRestartIndexOne() throws CommandException {
        List<String> output = completeBuffer("command2 stop yes ");

        assertTrue(output.contains("find"));
        assertTrue(output.contains("climb"));
        assertTrue(output.contains("stop"));
    }

    @Test
    public void testRestartIndexTwo() throws CommandException {
        List<String> output = completeBuffer("command1 list firstHalf ");

        assertTrue(output.contains("everything"));
        assertTrue(output.contains("nothing"));
        assertTrue(output.contains("firstHalf"));
        assertTrue(output.contains("secondHalf"));
    }

    @Test
    public void testParametersDoNotInterfereWithInlineCompletion() throws CommandException {
        List<String> inlineOutput = completeBuffer("command1 blah list blue red green notValid second");

        assertTrue(inlineOutput.contains("secondHalf"));
    }

    @Test
    public void testParametersDoNotInterfereWithCompletion() throws CommandException {
        List<String> output = completeBuffer("otherCommand blue green list red purple ");

        assertTrue(output.contains("parts"));
        assertTrue(output.contains("assemblies"));
        assertTrue(output.contains("degreesOfFreedom"));
        assertTrue(output.contains("bolts"));
        assertTrue(output.contains("tools"));
    }

    @Test
    public void testFileCompletion() throws CommandException, IOException {
        String filename1 = "testFilesNow";
        String filename2 = "document";
        String filename3 = "musicFile";
        String filename4 = "slideshow";

        createTempFile(filename1);
        createTempFile(filename2);
        createTempFile(filename3);
        createTempFile(filename4);

        List<String> output = completeBuffer("command2 find " + testDir.getAbsolutePath() + File.separator);

        assertTrue(output.contains(filename1));
        assertTrue(output.contains(filename2));
        assertTrue(output.contains(filename3));
        assertTrue(output.contains(filename4));
    }

    @Test
    public void testFileCompletionInline() throws CommandException, IOException {
        String filename1 = "testFilesNow";
        String filename2 = "document";
        String filename3 = "musicFile";
        String filename4 = "slideshow";

        createTempFile(filename1);
        createTempFile(filename2);
        createTempFile(filename3);
        createTempFile(filename4);

        List<String> output = completeBuffer("command2 find " + testDir.getAbsolutePath() + File.separator);

        assertTrue(output.contains(filename2));
    }

    @Test
    public void testCursorInMiddleOfWord() {
        List<String> output = completeBuffer("command1 ", 3);

        assertTrue(output.contains("command1"));
        assertTrue(output.contains("command2"));
        assertFalse(output.contains("otherCommand"));
        assertFalse(output.contains("anotherCommand"));
    }

    @Test
    public void testCursorBeforeWord() {
        List<String> output = completeBuffer("command1 ", 0);

        assertTrue(output.contains("command1"));
        assertTrue(output.contains("command2"));
        assertTrue(output.contains("otherCommand"));
        assertTrue(output.contains("anotherCommand"));
    }

    @Test
    public void testCursorBeforeSpace() {
        List<String> output = completeBuffer("command1 ", 8);

        assertTrue(output.contains("command1"));
        assertFalse(output.contains("command2"));
        assertFalse(output.contains("otherCommand"));
        assertFalse(output.contains("anotherCommand"));
    }

    @Test
    public void testCursorAtSpace() {
        List<String> output = completeBuffer("command1 ", 9);

        assertTrue(output.contains("list"));
        assertTrue(output.contains("create"));
        assertTrue(output.contains("delete"));
    }

    @Test
    public void testCursorAtSpaceBeforeAnotherWord() {
        List<String> output = completeBuffer("command1 list", 9);

        assertTrue(output.contains("list"));
        assertTrue(output.contains("create"));
        assertTrue(output.contains("delete"));
    }

    @Test
    public void testCursorInMiddleOfSecondWord() {
        List<String> output = completeBuffer("command1 list", 11);

        assertTrue(output.contains("list"));
        assertFalse(output.contains("create"));
        assertFalse(output.contains("delete"));
    }

    @Test
    public void testCursorLongerThanBufferLength() {
        List<CharSequence> candidates = new LinkedList<>();

        int cursor = tree.complete("command1 ", 503, candidates);
        List<String> convertedCandidates = convertToStringList(candidates);
        assertEquals(TreeCompleter.NOT_FOUND, cursor);
        assertTrue(convertedCandidates.isEmpty());
    }

    private List<String> completeBuffer(String buffer) {
        return completeBuffer(buffer, buffer.length());
    }

    private List<String> completeBuffer(String buffer, int cursor) {
        List<CharSequence> candidates = new LinkedList<>();

        tree.complete(buffer, cursor, candidates);
        List<String> convertedCandidates = convertToStringList(candidates);
        return convertedCandidates;
    }

    private List<String> convertToStringList(List<CharSequence> list) {
        List<String> stringsList = new ArrayList<>();
        for (CharSequence chars : list) {
            stringsList.add(chars.toString().trim());
        }
        return stringsList;
    }

    private void createTempFile(String name) throws IOException {
        File file = new File(testDir, name);
        file.deleteOnExit();
        file.createNewFile();
    }

    public void setupTreeCompleter() throws IOException {

        List<TreeCompleter.Node> commands = new ArrayList<>();
        TreeCompleter.Node command1 = createStringNode("command1");
        {
            TreeCompleter.Node create = createStringNode("create");
            TreeCompleter.Node delete = createStringNode("delete");
            TreeCompleter.Node list = createStringNode("list");
            {
                TreeCompleter.Node everything = createStringNode("everything");
                TreeCompleter.Node nothing = createStringNode("nothing");
                nothing.setRestartNode(command1);
                TreeCompleter.Node firstHalf = createStringNode("firstHalf");
                firstHalf.setRestartNode(list);
                TreeCompleter.Node secondHalf = createStringNode("secondHalf");
                secondHalf.setRestartNode(list); // maybe secondHalf
                list.addBranch(everything);
                list.addBranch(nothing);
                list.addBranch(firstHalf);
                list.addBranch(secondHalf);
            }
            command1.addBranch(list);
            command1.addBranch(create);
            command1.addBranch(delete);
        }

        TreeCompleter.Node command2 = createStringNode("command2");
        {
            TreeCompleter.Node find = createStringNode("find");
            {
                TreeCompleter.Node files = new TreeCompleter.Node(new FileNameCompleter());
                find.addBranch(files);
            }
            TreeCompleter.Node climb = createStringNode("climb");
            TreeCompleter.Node stop = createStringNode("stop");
            {
                TreeCompleter.Node yes = createStringNode("yes");
                yes.setRestartNode(command2);
                TreeCompleter.Node no = createStringNode("no");
                stop.addBranch(yes);
                stop.addBranch(no);
            }
            command2.addBranch(find);
            command2.addBranch(climb);
            command2.addBranch(stop);
        }
        TreeCompleter.Node otherCommand = createStringNode("otherCommand");
        {
            TreeCompleter.Node list = createStringNode("list");
            {
                TreeCompleter.Node parts = createStringNode("parts");
                TreeCompleter.Node assemblies = createStringNode("assemblies");
                assemblies.setRestartNode(otherCommand);
                TreeCompleter.Node degreesOfFreedom = createStringNode("degreesOfFreedom");
                degreesOfFreedom.setRestartNode(list);
                TreeCompleter.Node bolts = createStringNode("bolts");
                bolts.setRestartNode(list);
                TreeCompleter.Node tools = createStringNode("tools");
                tools.setRestartNode(list);
                list.addBranch(parts);
                list.addBranch(assemblies);
                list.addBranch(degreesOfFreedom);
                list.addBranch(bolts);
                list.addBranch(tools);
            }
            TreeCompleter.Node create = createStringNode("create");
            {
                TreeCompleter.Node a = createStringNode("a");
                TreeCompleter.Node longNode = createStringNode("long");
                TreeCompleter.Node chain = createStringNode("chain");
                TreeCompleter.Node of = createStringNode("of");
                TreeCompleter.Node tab = createStringNode("tab");
                TreeCompleter.Node completing = createStringNode("completing");
                TreeCompleter.Node words = createStringNode("words");
                completing.addBranch(words);
                tab.addBranch(completing);
                of.addBranch(tab);
                chain.addBranch(of);
                longNode.addBranch(chain);
                a.addBranch(longNode);
                create.addBranch(a);
            }
            TreeCompleter.Node start = createStringNode("start");
            {
                TreeCompleter.Node yes = createStringNode("yes");
                yes.setRestartNode(otherCommand);
                TreeCompleter.Node no = createStringNode("no");
                start.addBranch(yes);
                start.addBranch(no);
            }
            otherCommand.addBranch(list);
            otherCommand.addBranch(create);
            otherCommand.addBranch(start);
        }
        TreeCompleter.Node anotherCommand = createStringNode("anotherCommand");
        {
            TreeCompleter.Node list = createStringNode("list");
            {
                TreeCompleter.Node everything = createStringNode("everything");
                TreeCompleter.Node nothing = createStringNode("nothing");
                TreeCompleter.Node firstHalf = createStringNode("firstHalf");
                TreeCompleter.Node secondHalf = createStringNode("secondHalf");
                TreeCompleter.Node twentyFifthToSeventyFifthPart = createStringNode("25to75part");
                list.addBranch(everything);
                list.addBranch(nothing);
                list.addBranch(firstHalf);
                list.addBranch(secondHalf);
                list.addBranch(twentyFifthToSeventyFifthPart);
            }
            anotherCommand.addBranch(list);
        }

        commands.add(command1);
        commands.add(command2);
        commands.add(otherCommand);
        commands.add(anotherCommand);

        tree.addBranches(commands);
    }

}
