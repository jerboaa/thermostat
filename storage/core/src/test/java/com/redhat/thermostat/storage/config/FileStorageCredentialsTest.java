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

package com.redhat.thermostat.storage.config;

import java.io.File;
import java.io.Reader;
import java.io.StringReader;

import org.junit.Assert;
import org.junit.Test;

import com.redhat.thermostat.storage.config.FileStorageCredentials;

public class FileStorageCredentialsTest {
    
    @Test
    public void testAuthConfigNoNewlineAtEOF() {
        Reader reader = new StringReader("username=user\npassword=pass");
        FileStorageCredentials creds = new FileStorageCredentials(reader);
        Assert.assertEquals("user", creds.getUsername());
        Assert.assertEquals(4, creds.getPassword().length);
        Assert.assertEquals("pass", new String(creds.getPassword()));
    }

    @Test
    public void testAuthConfig() {
        Reader reader = new StringReader("username=user\npassword=pass\n");
        FileStorageCredentials creds = new FileStorageCredentials(reader);
        Assert.assertEquals("user", creds.getUsername());
        Assert.assertEquals("pass", new String(creds.getPassword()));
    }

    @Test
    public void testEmptyAuthConfig() {
        Reader reader = new StringReader("");
        FileStorageCredentials creds = new FileStorageCredentials(reader);
        Assert.assertNull(creds.getUsername());
        Assert.assertNull(creds.getPassword());
    }

    @Test
    public void testNonExistingAgentAuthFile() {
        File file = new File("this.file.should.not.exist");
        FileStorageCredentials creds = new FileStorageCredentials(file);
        Assert.assertNull(creds.getUsername());
        Assert.assertNull(creds.getPassword());
    }

    @Test
    public void testAuthConfigWithConfigCommentedOut() {
        Reader reader = new StringReader("#username=user\n#password=pass\n");
        FileStorageCredentials creds = new FileStorageCredentials(reader);
        Assert.assertNull(creds.getUsername());
        Assert.assertNull(creds.getPassword());
    }

    @Test
    public void testAuthConfigWithVariousExtraNewlines() {
        Reader reader = new StringReader("\n#username=nottheuser\n\n\n#password=wrong\nusername=user\n\n\npassword=pass\n\n\n#username=wronguser\n\n\n#password=badpassword");
        FileStorageCredentials creds = new FileStorageCredentials(reader);
        Assert.assertEquals("user", creds.getUsername());
        Assert.assertEquals("pass", new String(creds.getPassword()));
    }

    @Test
    public void testAuthConfigWithSillyWhitespace() {
        Reader reader = new StringReader("\tusername =\t  user\n\n\npassword=pass   \n\n\n");
        FileStorageCredentials creds = new FileStorageCredentials(reader);
        Assert.assertEquals("user", creds.getUsername());
        Assert.assertEquals("pass", new String(creds.getPassword()));
    }

    @Test
    public void testCommentPrecededByWhitespaceIsStillIgnored() {
        Reader reader = new StringReader("     #username=wronguser\nusername=user\npassword=pass\n    #username=wronguser");
        FileStorageCredentials creds = new FileStorageCredentials(reader);
        Assert.assertEquals("user", creds.getUsername());
        Assert.assertEquals("pass", new String(creds.getPassword()));
    }

    @Test
    public void testAuthParamsContainingWhitespace() {
        Reader reader = new StringReader("     #username=wronguser\nusername=u s e r\npassword=p a s s\n    #username=wronguser");
        FileStorageCredentials creds = new FileStorageCredentials(reader);
        Assert.assertEquals("u s e r", creds.getUsername());
        Assert.assertEquals("p a s s", new String(creds.getPassword()));
    }

    @Test
    public void testAlternateNewLine() {
        Reader reader = new StringReader("username=user\r\npassword=pass\r\n");
        FileStorageCredentials creds = new FileStorageCredentials(reader, "\r\n");
        Assert.assertEquals("user", creds.getUsername());
        Assert.assertEquals("pass", new String(creds.getPassword()));
    }

}

