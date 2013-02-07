<?xml version="1.0"?>

<!--

 Copyright 2012, 2013 Red Hat, Inc.

 This file is part of Thermostat.

 Thermostat is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published
 by the Free Software Foundation; either version 2, or (at your
 option) any later version.

 Thermostat is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Thermostat; see the file COPYING.  If not see
 <http://www.gnu.org/licenses />.

 Linking this code with other modules is making a combined work
 based on this code.  Thus, the terms and conditions of the GNU
 General Public License cover the whole combination.

 As a special exception, the copyright holders of this code give
 you permission to link this code with independent modules to
 produce an executable, regardless of the license terms of these
 independent modules, and to copy and distribute the resulting
 executable under terms of your choice, provided that you also
 meet, for each linked independent module, the terms and conditions
 of the license of that module.  An independent module is a module
 which is not derived from or based on this code.  If you modify
 this code, you may extend this exception to your version of the
 library, but you are not obligated to do so.  If you do not wish
 to do so, delete this exception statement from your version.

-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="html" indent="yes" encoding="UTF-8"/>

  <xsl:template match="/plugin-docs">
    <xsl:text disable-output-escaping="yes">&lt;!DOCTYPE html&gt;</xsl:text>
    <html>
      <head>
        <title>Plugin Documentation For Thermostat</title>

        <style>
            body { background: #f9f9f9; font-family: sans-serif; font-size: 10pt; }
            table { width: 90%; border-width:0; border-collapse: collapse; }
            thead { font-weight: bold }
            thead tr:nth-child(odd) { background: #f9f9f9; }
            tr:nth-child(odd) { background: #e0e0e0; }
            td { vertical-align: top; padding: 0.5em 1em }
            .description { margin: 1em; }
            .javadoc { margin: 0; padding:0; }
            .javadoc h5, .javadoc h6, .javadoc h7 { display:inline; }
        </style>
      </head>

      <body>
        <h1>Points of Interest for Plugin Developers</h1>

        <p>Jump to</p>
        <ol>
          <li><a href="#extension-points">Extension Points</a></li>
          <li><a href="#services">Services</a></li>
        </ol>

        <h2><a id="extension-points">Extension Points</a></h2>
        <div class="description">
          <p>
          <xsl:apply-templates select="meta[contains(name/text(), 'ExtensionPoint')]"/>
          </p>
        </div>
        <table>
          <thead>
            <tr>
              <td>Extension Point Name</td>
              <td>Documentation</td>
            </tr>
          </thead>
          <tbody>
            <xsl:apply-templates select="extension-point">
              <xsl:sort select="name" />
            </xsl:apply-templates>
          </tbody>
        </table>

        <h2><a id="services">Services</a></h2>
        <div class="description">
          <p>
          <xsl:apply-templates select="meta[contains(name/text(), 'Service')]"/>
          </p>
        </div>
        <table>
          <thead>
            <tr>
              <td>Service Name</td>
              <td>Documentation</td>
            </tr>
          </thead>
          <tbody>
            <xsl:apply-templates select="service">
              <xsl:sort select="name" />
            </xsl:apply-templates>
          </tbody>
        </table>

      </body>
    </html>
  </xsl:template>

  <xsl:template match="extension-point|service">
    <tr>
      <td>
        <code class="point"> <xsl:value-of select="name" /> </code>
      </td>
      <td>
        <div class="javadoc">
          <xsl:apply-templates select="doc" />
        </div>
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="meta">
    <xsl:apply-templates select="doc" />
  </xsl:template>

  <xsl:template match="doc">
    <xsl:copy-of select="."/>
  </xsl:template>

</xsl:stylesheet>

