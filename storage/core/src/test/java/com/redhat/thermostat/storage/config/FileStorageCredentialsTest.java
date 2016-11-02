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

package com.redhat.thermostat.storage.config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class FileStorageCredentialsTest {
    
    private File credentialsFile = null;

    private void createCredentialsFile(String contents) throws IOException {
        createCredentialsFile(contents.getBytes(StandardCharsets.US_ASCII));
    }

    private void createCredentialsFile(byte[] contents) throws IOException {
        Path tempFile = Files.createTempFile("auth.config", "test");
        Files.write(tempFile, contents, StandardOpenOption.TRUNCATE_EXISTING);
        credentialsFile = tempFile.toFile();
    }

    @After
    public void cleanup() {
        if (credentialsFile != null && credentialsFile.exists()) {
            credentialsFile.delete();
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNullAuthFile() throws Exception {
        new FileStorageCredentials(null);
    }

    @Test
    public void testNonExistingAgentAuthFile() throws Exception {
        credentialsFile = new File("this.file.should.not.exist");
        FileStorageCredentials creds = new FileStorageCredentials(credentialsFile);
        Assert.assertNull(creds.getUsername());
        Assert.assertNull(creds.getPassword());
    }

    @Test
    public void testAuthConfigNoNewlineAtEOF() throws Exception {
        createCredentialsFile("username=user\npassword=pass");
        FileStorageCredentials creds = new FileStorageCredentials(credentialsFile);
        Assert.assertEquals("user", creds.getUsername());
        Assert.assertEquals(4, creds.getPassword().length);
        Assert.assertEquals("pass", new String(creds.getPassword()));
    }

    @Test
    public void testAuthConfigCanReadWindows() throws Exception {
        createCredentialsFile("username=user\r\npassword=pass");
        FileStorageCredentials creds = new FileStorageCredentials(credentialsFile);
        Assert.assertEquals("user", creds.getUsername());
        Assert.assertEquals(4, creds.getPassword().length);
        Assert.assertEquals("pass", new String(creds.getPassword()));
    }

    @Test
    public void testAuthConfigCanReadReversed() throws Exception {
        createCredentialsFile("username=user\n\rpassword=pass\n");
        FileStorageCredentials creds = new FileStorageCredentials(credentialsFile);
        Assert.assertEquals("user", creds.getUsername());
        Assert.assertEquals(4, creds.getPassword().length);
        Assert.assertEquals("pass", new String(creds.getPassword()));
    }

    @Test
    public void testAuthConfigCanReadOldschoolMac() throws Exception {
        createCredentialsFile("username=user\r\rpassword=pass\r");
        FileStorageCredentials creds = new FileStorageCredentials(credentialsFile);
        Assert.assertEquals("user", creds.getUsername());
        Assert.assertEquals(4, creds.getPassword().length);
        Assert.assertEquals("pass", new String(creds.getPassword()));
    }

    @Test
    public void testAuthConfig() throws Exception {
        createCredentialsFile("username=user\npassword=pass\n");
        FileStorageCredentials creds = new FileStorageCredentials(credentialsFile);
        Assert.assertEquals("user", creds.getUsername());
        Assert.assertEquals("pass", new String(creds.getPassword()));
    }

    @Test
    public void testEmptyAuthConfig() throws Exception {
        createCredentialsFile("");
        FileStorageCredentials creds = new FileStorageCredentials(credentialsFile);
        Assert.assertNull(creds.getUsername());
        Assert.assertNull(creds.getPassword());
    }

    @Test
    public void testAuthConfigWithConfigCommentedOut() throws Exception {
        createCredentialsFile("#username=user\n#password=pass\n");
        FileStorageCredentials creds = new FileStorageCredentials(credentialsFile);
        Assert.assertNull(creds.getUsername());
        Assert.assertNull(creds.getPassword());
    }

    @Test
    public void testAuthConfigWithVariousExtraNewlines() throws Exception {
        createCredentialsFile("\n#username=nottheuser\n\n\n#password=wrong\nusername=user\n\n\npassword=pass\n\n\n#username=wronguser\n\n\n#password=badpassword");
        FileStorageCredentials creds = new FileStorageCredentials(credentialsFile);
        Assert.assertEquals("user", creds.getUsername());
        Assert.assertEquals("pass", new String(creds.getPassword()));
    }

    @Test
    public void testAuthConfigWithSillyWhitespace() throws Exception {
        createCredentialsFile("\tusername =\t  user\n\n\npassword=pass   \n\n\n");
        FileStorageCredentials creds = new FileStorageCredentials(credentialsFile);
        Assert.assertEquals("user", creds.getUsername());
        Assert.assertEquals("pass", new String(creds.getPassword()));
    }

    @Test
    public void testCommentPrecededByWhitespaceIsStillIgnored() throws Exception {
        createCredentialsFile("     #username=wronguser\nusername=user\npassword=pass\n    #username=wronguser");
        FileStorageCredentials creds = new FileStorageCredentials(credentialsFile);
        Assert.assertEquals("user", creds.getUsername());
        Assert.assertEquals("pass", new String(creds.getPassword()));
    }

    @Test
    public void testAuthParamsContainingWhitespace() throws Exception {
        createCredentialsFile("     #username=wronguser\nusername=u s e r\npassword=p a s s\n    #username=wronguser");
        FileStorageCredentials creds = new FileStorageCredentials(credentialsFile);
        Assert.assertEquals("u s e r", creds.getUsername());
        Assert.assertEquals("p a s s", new String(creds.getPassword()));
    }

    @Test
    public void testAlternateNewLine() throws Exception {
        createCredentialsFile("username=user\r\npassword=pass\r\n");
        FileStorageCredentials creds = new FileStorageCredentials(credentialsFile);
        Assert.assertEquals("user", creds.getUsername());
        Assert.assertEquals("pass", new String(creds.getPassword()));
    }

    @Test
    public void testArrayCompare() throws Exception {
        String data = "username=user\npassword=pass\n";
        createCredentialsFile(data);
        FileStorageCredentials creds = new FileStorageCredentials(credentialsFile);
        char[] result = creds.getValueFromData(data.toCharArray(), data.length(), new char[] { 'u', 's', 'e', 'r', 'n', 'a', 'm', 'e'});
        Assert.assertArrayEquals("user".toCharArray(), result);
    }

    @Test
    public void testAuthConfigWithUnexpectedEncoding() throws Exception {
        createCredentialsFile("\nusername=\u0220\n\n\npassword=\u0220".getBytes(StandardCharsets.UTF_8));
        FileStorageCredentials creds = new FileStorageCredentials(credentialsFile);
        final String REPLACEMENT_CHARACTER = "\ufffd";
        // decoder fails to parse input, replaces each ascii character with REPLACEMENT_CHARACTER
        Assert.assertEquals(REPLACEMENT_CHARACTER + REPLACEMENT_CHARACTER, creds.getUsername());
        Assert.assertEquals(REPLACEMENT_CHARACTER + REPLACEMENT_CHARACTER, new String(creds.getPassword()));
    }

}

