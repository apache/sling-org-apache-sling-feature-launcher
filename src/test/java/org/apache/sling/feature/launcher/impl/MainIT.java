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
import static org.mockito.Mockito.when;

import java.security.Permission;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.sling.feature.ArtifactId;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/** Run this as part of integration tests to help make sure we
 *  embed the right Apache commons classes.
*/
public class MainIT {

    protected static class SystemExitException extends SecurityException {

        private static final long serialVersionUID = 1L;

        public final int status;

        public SystemExitException(int status) {

            super("NoExit");
            this.status = status;
        }
    }

    private static class NoSystemExitSecurityManager extends SecurityManager {

        @Override
        public void checkPermission(Permission perm) {

            // allow anything.
        }

        @Override
        public void checkPermission(Permission perm, Object context) {

            // allow anything.
        }

        @Override
        public void checkExit(int status) {

            super.checkExit(status);
            throw new SystemExitException(status);
        }
    }

    @BeforeClass
    public static void setUp() throws Exception {

        System.setSecurityManager(new NoSystemExitSecurityManager());
    }

    @AfterClass
    public static void tearDown() throws Exception {

        System.setSecurityManager(null);
    }

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
    public void testParseContainer() {

        Options os = mock(Options.class);
        Option o = mock(Option.class);
        when(os.getOption(Main.OPT_PRINT_CONTAINER_ENV_HELP)).thenReturn(o);

    }

    @Test
    public void testParseVerbose() {

        Main.parseArgs(noActionAllowesConfig, new String[] { "-" + Main.OPT_VERBOSE, "debug" });
        assertEquals("debug", System.getProperty("org.slf4j.simpleLogger.defaultLogLevel"));

        Main.parseArgs(noActionAllowesConfig, new String[] { "-" + Main.OPT_VERBOSE });
        assertEquals("debug", System.getProperty("org.slf4j.simpleLogger.defaultLogLevel"));

        Main.parseArgs(noActionAllowesConfig, new String[] { "-" + Main.OPT_VERBOSE, "warn" });
        assertEquals("warn", System.getProperty("org.slf4j.simpleLogger.defaultLogLevel"));

    }

    @Test
    public void testParseHome() {

        Main.parseArgs(noActionAllowesConfig, new String[] { "-" + Main.OPT_HOME_DIR });

        LauncherConfig config = new LauncherConfig();
        Main.parseArgs(config, new String[] { "-" + Main.OPT_HOME_DIR, "foo" });
        assertEquals("foo", config.getHomeDirectory().toString());

    }

    @Test
    public void testParseCacheDir() {

        Main.parseArgs(noActionAllowesConfig, new String[] { "-" + Main.OPT_CACHE_DIR });

        LauncherConfig config = new LauncherConfig();
        Main.parseArgs(config, new String[] { "-" + Main.OPT_CACHE_DIR, "foo" });
        assertEquals("foo", config.getCacheDirectory().toString());

    }

    @Test
    public void testParseFelixFwVersion() {

        Main.parseArgs(noActionAllowesConfig,
                new String[] { "-" + Main.OPT_FELIX_FRAMEWORK_VERSION });

        LauncherConfig config = new LauncherConfig();
        Main.parseArgs(config, new String[] { "-" + Main.OPT_FELIX_FRAMEWORK_VERSION, "foo" });
        assertEquals("foo", config.getFrameworkVersion().toString());

    }

    @Test
    public void testParseOSGiFwArtifact() {

        Main.parseArgs(noActionAllowesConfig,
                new String[] { "-" + Main.OPT_OSGI_FRAMEWORK_ARTIFACT });

        LauncherConfig config = new LauncherConfig();
        Main.parseArgs(config, new String[] { "-" + Main.OPT_OSGI_FRAMEWORK_ARTIFACT, "foo" });
        assertEquals("foo", config.getFrameworkArtifact());

    }

    @Test
    public void testParse_Config_Clash() {

        Main.parseArgs(noActionAllowesConfig, new String[] { "-" + Main.OPT_CONFIG_CLASH });

        LauncherConfig config = new LauncherConfig();
        // -C and -CC may have a conflict
        // Main.parseArgs(config, new String[] { "-" + Main.OPT_CONFIG_CLASH + "a=1",
        //         "-" + Main.OPT_CONFIG_CLASH + "b=2" });
        //
        // assertTrue(config.getConfigClashOverrides().containsKey("a"));
        // assertEquals("1", config.getConfigClashOverrides().get("a"));
        // assertTrue(config.getConfigClashOverrides().containsKey("b"));
        // assertEquals("2", config.getConfigClashOverrides().get("b"));

        config = new LauncherConfig();
        Main.parseArgs(config, new String[] { "-" + Main.OPT_CONFIG_CLASH, "a", "1",
                "-" + Main.OPT_CONFIG_CLASH, "b", "2" });

        assertTrue(config.getConfigClashOverrides().containsKey("a"));
        assertEquals("1", config.getConfigClashOverrides().get("a"));
        assertTrue(config.getConfigClashOverrides().containsKey("b"));
        assertEquals("2", config.getConfigClashOverrides().get("b"));

    }

    @Test
    public void testParse_OptVarVal() {

        Main.parseArgs(noActionAllowesConfig, new String[] { "-" + Main.OPT_VARIABLE_VALUES });

        LauncherConfig config = new LauncherConfig();
        Main.parseArgs(config, new String[] { "-" + Main.OPT_VARIABLE_VALUES + "a=1",
                "-" + Main.OPT_VARIABLE_VALUES + "b=2" });

        assertTrue(config.getVariables().containsKey("a"));
        assertEquals("1", config.getVariables().get("a"));
        assertTrue(config.getVariables().containsKey("b"));
        assertEquals("2", config.getVariables().get("b"));

        config = new LauncherConfig();
        Main.parseArgs(config, new String[] { "-" + Main.OPT_VARIABLE_VALUES, "a", "1",
                "-" + Main.OPT_VARIABLE_VALUES, "b", "2" });

        assertTrue(config.getVariables().containsKey("a"));
        assertEquals("1", config.getVariables().get("a"));
        assertTrue(config.getVariables().containsKey("b"));
        assertEquals("2", config.getVariables().get("b"));

    }

    @Test
    public void testParse_FW_Prop() {

        Main.parseArgs(noActionAllowesConfig, new String[] { "-" + Main.OPT_FRAMEWORK_PROPERTIES });

        LauncherConfig config = new LauncherConfig();
        Main.parseArgs(config, new String[] { "-" + Main.OPT_FRAMEWORK_PROPERTIES + "a=1",
                "-" + Main.OPT_FRAMEWORK_PROPERTIES + "b=2" });

        assertTrue(config.getInstallation().getFrameworkProperties().containsKey("a"));
        assertEquals("1", config.getInstallation().getFrameworkProperties().get("a"));

        assertTrue(config.getInstallation().getFrameworkProperties().containsKey("b"));
        assertEquals("2", config.getInstallation().getFrameworkProperties().get("b"));
    }

    @Test
    public void testParse_Artifact_Clash() {

        Main.parseArgs(noActionAllowesConfig, new String[] { "-" + Main.OPT_ARTICACT_CLASH });

        LauncherConfig config = new LauncherConfig();
        Main.parseArgs(config, new String[] { "-" + Main.OPT_ARTICACT_CLASH, "foo:bar:1" });
        assertTrue(config.getArtifactClashOverrides().contains(ArtifactId.parse("foo:bar:1")));

        config = new LauncherConfig();
        Main.parseArgs(config,
                new String[] { "-" + Main.OPT_ARTICACT_CLASH, "foo:bar:1", "foo:bar:2" });
        assertTrue(config.getArtifactClashOverrides().contains(ArtifactId.parse("foo:bar:1")));
        assertTrue(config.getArtifactClashOverrides().contains(ArtifactId.parse("foo:bar:2")));

        config = new LauncherConfig();
        Main.parseArgs(config, new String[] { "-" + Main.OPT_ARTICACT_CLASH, "foo:bar:1",
                "-" + Main.OPT_ARTICACT_CLASH, "foo:bar:2" });
        assertTrue(config.getArtifactClashOverrides().contains(ArtifactId.parse("foo:bar:1")));
        assertTrue(config.getArtifactClashOverrides().contains(ArtifactId.parse("foo:bar:2")));

    }

    @Test
    public void testParse_RepoUrls() {

        Main.parseArgs(noActionAllowesConfig, new String[] { "-" + Main.OPT_REPOSITORY_URLS });

        LauncherConfig config = new LauncherConfig();
        Main.parseArgs(config, new String[] { "-" + Main.OPT_REPOSITORY_URLS, "foo" });
        assertArrayEquals(config.getRepositoryUrls(), new Object[] { "foo" });

        config = new LauncherConfig();
        Main.parseArgs(config, new String[] { "-" + Main.OPT_REPOSITORY_URLS, "foo", "bar" });
        assertArrayEquals(config.getRepositoryUrls(), new Object[] { "foo", "bar" });

        config = new LauncherConfig();
        Main.parseArgs(config, new String[] { "-" + Main.OPT_REPOSITORY_URLS, "foo",
                "-" + Main.OPT_REPOSITORY_URLS, "bar" });
        assertArrayEquals(config.getRepositoryUrls(), new Object[] { "foo", "bar" });

    }

    @Test
    public void testParse_FeatureFiles() {

        Main.parseArgs(noActionAllowesConfig, new String[] { "-" + Main.OPT_FEATURE_FILES });

        LauncherConfig config = new LauncherConfig();
        Main.parseArgs(config, new String[] { "-" + Main.OPT_FEATURE_FILES, "foo" });
        assertTrue(config.getFeatureFiles().contains("foo"));

        config = new LauncherConfig();
        Main.parseArgs(config, new String[] { "-" + Main.OPT_FEATURE_FILES, "foo", "bar" });
        assertTrue(config.getFeatureFiles().contains("foo"));
        assertTrue(config.getFeatureFiles().contains("bar"));

        config = new LauncherConfig();
        Main.parseArgs(config, new String[] { "-" + Main.OPT_FEATURE_FILES, "foo",
                "-" + Main.OPT_FEATURE_FILES, "bar" });
        assertTrue(config.getFeatureFiles().contains("foo"));
        assertTrue(config.getFeatureFiles().contains("bar"));

    }

    @Test
    public void testMain_main() {

        try {
            Main.main(new String[] {});
        } catch (SystemExitException e) {
            assertEquals("Exit status", 1, e.status);
        }

    }
}
