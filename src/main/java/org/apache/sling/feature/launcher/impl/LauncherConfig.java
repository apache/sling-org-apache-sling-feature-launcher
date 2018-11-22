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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.sling.feature.io.file.ArtifactManagerConfig;
import org.apache.sling.feature.io.file.spi.ArtifactProviderContext;

/**
 * This class holds the configuration of the launcher.
 */
public class LauncherConfig
    extends ArtifactManagerConfig
    implements ArtifactProviderContext {

    private static final String HOME = "launcher";

    private static final String CACHE_DIR = "cache";

    private final List<String> artifactClashOverrides = new ArrayList<>();

    /** The feature files or directories. */
    private final LinkedHashSet<String> featureFiles = new LinkedHashSet<>();

    private final Installation installation = new Installation();

    private volatile File home = new File(HOME);

    private final Map<String,String> variables = new HashMap<>();

    /**
     * Create a new configuration object.
     * Set the default values
     */
    public LauncherConfig() {
        this.setCacheDirectory(new File(getHomeDirectory(), CACHE_DIR));
    }

    public List<String> getArtifactClashOverrides() {
        return this.artifactClashOverrides;
    }

    /**
     * Set the list of feature files or directories.
     * @param featureFiles The array with the feature file names.
     */
    public void addFeatureFiles(final String... featureFiles) {
        this.featureFiles.addAll(Arrays.asList(featureFiles));
    }

    /**
     * Get the list of feature files.
     * @return The array of names.
     */
    public String[] getFeatureFiles() {
        return this.featureFiles.toArray(new String[0]);
    }

    /**
     * Get the home directory.
     * @return The home directory.
     */
    public File getHomeDirectory() {
        return this.home;
    }

    public void setHomeDirectory(File file) {
        this.home = file;
    }

    public Installation getInstallation() {
        return this.installation;
    }

    /**
     * Clear all in-memory objects
     */
    public void clear() {
        this.installation.clear();
    }

    public Map<String,String> getVariables() {
        return this.variables;
    }
}
