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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.io.artifacts.ArtifactManagerConfig;

/**
 * This class holds the configuration of the launcher.
 */
public class LauncherConfig
    extends ArtifactManagerConfig {

    private static final String HOME = "launcher";

    private static final String CACHE_DIR = "cache";

    private final List<ArtifactId> artifactClashOverrides = new ArrayList<>();

    private final Map<String,String> configClashOverrides = new LinkedHashMap<>();

    private final Map<String, Map<String,String>> extensionConfiguration = new HashMap<>();

    /** The feature files or directories. */
    private final LinkedHashSet<String> featureFiles = new LinkedHashSet<>();

    private final Installation installation = new Installation();

    private volatile File home = new File(HOME);

    private final Map<String,String> variables = new HashMap<>();

    private volatile String frameworkVersion;

    private volatile String frameworkArtifact;

    private volatile ArtifactId launchFeatureId;

    private volatile boolean cacheOnly = false;

    /**
     * Create a new configuration object.
     * Set the default values
     */
    public LauncherConfig() {
        this.setCacheDirectory(new File(getHomeDirectory(), CACHE_DIR));
        this.launchFeatureId = ArtifactId.parse("group:assembled:1.0.0");
    }

    public List<ArtifactId> getArtifactClashOverrides() {
        return this.artifactClashOverrides;
    }

    public Map<String, String> getConfigClashOverrides() {
        return this.configClashOverrides;
    }

    public Map<String, Map<String, String>> getExtensionConfiguration() {
        return this.extensionConfiguration;
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
    public Collection<String> getFeatureFiles() {
        return this.featureFiles;
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

    public Map<String,String> getVariables() {
        return this.variables;
    }

    public String getFrameworkVersion() {
        return frameworkVersion;
    }

    public void setFrameworkVersion(final String frameworkVersion) {
        this.frameworkVersion = frameworkVersion;
    }

    public String getFrameworkArtifact() {
        return frameworkArtifact;
    }

    public void setFrameworkArtifact(final String frameworkArtifact) {
        this.frameworkArtifact = frameworkArtifact;
    }

    /**
     * Set the feature id for the launch feature
     * @param id The id
     * @throws IllegalArgumentException If the id is invalid
     */
    public void setLaunchFeatureId(final String id) {
        this.launchFeatureId = ArtifactId.parse(id);
    }

    /**
     * Get the feature id for the launch feature
     * @return The feature id
     */
    public ArtifactId getLaunchFeatureId() {
        return this.launchFeatureId;
    }

    public boolean getCacheOnly() {
        return this.cacheOnly;
    }

    public void setCacheOnly(final boolean value) {
        this.cacheOnly = value;
    }
}
