/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.feature.launcher.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.feature.ArtifactId;
import org.junit.Test;

public class MainTest {

    @Test
    public void testSplitCommandlineArgs() {

        assertArrayEquals(new String[] { "hi", "ho" }, Main.splitKeyVal("hi=ho"));
        assertArrayEquals(new String[] { "hi.de.hi", "true" }, Main.splitKeyVal("hi.de.hi"));
    }

    @Test
    public void testSplitMapCommandlineArgs() {

        assertEquals(new AbstractMap.SimpleEntry<>("foo", Collections.singletonMap("bar", "tar")),
                Main.splitMap("foo:bar=tar"));

        assertEquals(new AbstractMap.SimpleEntry<>("hello", Collections.emptyMap()),
                Main.splitMap("hello"));

        Map<String, String> em = new HashMap<>();
        em.put("a.b.c", "d.e.f");
        em.put("h.i.j", "k.l.m");
        Map.Entry<String, Map<String, String>> e = new AbstractMap.SimpleEntry<>("ding.dong", em);
        assertEquals(e, Main.splitMap("ding.dong:a.b.c=d.e.f,h.i.j=k.l.m"));
    }

    LauncherConfig noActionAllowesConfig = mock(LauncherConfig.class, invocationOnMock -> {
        throw new RuntimeException(invocationOnMock.getMethod().getName());
    });

    @Test
    public void testParse() {

        Main.parseArgs(noActionAllowesConfig, new String[] {});
    }

    @Test
    public void testParseVerbose() {

        Main.parseArgs(noActionAllowesConfig, new String[] { "-v", "debug" });
        assertEquals("debug", System.getProperty("org.slf4j.simpleLogger.defaultLogLevel"));

        Main.parseArgs(noActionAllowesConfig, new String[] { "-v" });
        assertEquals("debug", System.getProperty("org.slf4j.simpleLogger.defaultLogLevel"));

        Main.parseArgs(noActionAllowesConfig, new String[] { "-v", "warn" });
        assertEquals("warn", System.getProperty("org.slf4j.simpleLogger.defaultLogLevel"));

    }

    @Test
    public void testParseHome() {

        Main.parseArgs(noActionAllowesConfig, new String[] { "-p" });

        LauncherConfig config = new LauncherConfig();
        Main.parseArgs(config, new String[] { "-p", "foo" });
        assertEquals("foo", config.getHomeDirectory().toString());

    }

    @Test
    public void testParseCacheDir() {

        Main.parseArgs(noActionAllowesConfig, new String[] { "-c" });

        LauncherConfig config = new LauncherConfig();
        Main.parseArgs(config, new String[] { "-c", "foo" });
        assertEquals("foo", config.getCacheDirectory().toString());

    }

    @Test
    public void testParseFelixFwVersion() {

        Main.parseArgs(noActionAllowesConfig, new String[] { "-fv" });

        LauncherConfig config = new LauncherConfig();
        Main.parseArgs(config, new String[] { "-fv", "foo" });
        assertEquals("foo", config.getFrameworkVersion().toString());

    }

    @Test
    public void testParseOSGiFwArtifact() {

        Main.parseArgs(noActionAllowesConfig, new String[] { "-fa" });

        LauncherConfig config = new LauncherConfig();
        Main.parseArgs(config, new String[] { "-fa", "foo" });
        assertEquals("foo", config.getFrameworkArtifact());

    }

    @Test
    public void testParse_Artifact_Clash() {

        Main.parseArgs(noActionAllowesConfig, new String[] { "-C" });

        LauncherConfig config = new LauncherConfig();
        Main.parseArgs(config, new String[] { "-C", "foo:bar:1" });
        assertTrue(config.getArtifactClashOverrides().contains(ArtifactId.parse("foo:bar:1")));

        config = new LauncherConfig();
        Main.parseArgs(config, new String[] { "-C", "foo:bar:1", "foo:bar:2" });
        assertTrue(config.getArtifactClashOverrides().contains(ArtifactId.parse("foo:bar:1")));
        assertTrue(config.getArtifactClashOverrides().contains(ArtifactId.parse("foo:bar:2")));

        config = new LauncherConfig();
        Main.parseArgs(config, new String[] { "-C", "foo:bar:1", "-C", "foo:bar:2" });
        assertTrue(config.getArtifactClashOverrides().contains(ArtifactId.parse("foo:bar:1")));
        assertTrue(config.getArtifactClashOverrides().contains(ArtifactId.parse("foo:bar:2")));

    }

    @Test
    public void testParse_RepoUrls() {

        Main.parseArgs(noActionAllowesConfig, new String[] { "-u" });

        LauncherConfig config = new LauncherConfig();
        Main.parseArgs(config, new String[] { "-u", "foo" });
        assertArrayEquals(config.getRepositoryUrls(), new Object[] { "foo" });

        config = new LauncherConfig();
        Main.parseArgs(config, new String[] { "-u", "foo", "bar" });
        assertArrayEquals(config.getRepositoryUrls(), new Object[] { "foo", "bar" });

        config = new LauncherConfig();
        Main.parseArgs(config, new String[] { "-u", "foo", "-u", "bar" });
        assertArrayEquals(config.getRepositoryUrls(), new Object[] { "foo", "bar" });

    }

    @Test
    public void testParse_FeatureFiles() {

        Main.parseArgs(noActionAllowesConfig, new String[] { "-f" });

        LauncherConfig config = new LauncherConfig();
        Main.parseArgs(config, new String[] { "-f", "foo" });
        assertTrue(config.getFeatureFiles().contains("foo"));

        config = new LauncherConfig();
        Main.parseArgs(config, new String[] { "-f", "foo", "bar" });
        assertTrue(config.getFeatureFiles().contains("foo"));
        assertTrue(config.getFeatureFiles().contains("bar"));

        config = new LauncherConfig();
        Main.parseArgs(config, new String[] { "-f", "foo", "-f", "bar" });
        assertTrue(config.getFeatureFiles().contains("foo"));
        assertTrue(config.getFeatureFiles().contains("bar"));

    }

}
