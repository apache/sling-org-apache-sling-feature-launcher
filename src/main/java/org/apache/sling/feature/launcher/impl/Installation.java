/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.feature.launcher.impl;

import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.feature.launcher.spi.LauncherRunContext;
import org.slf4j.Logger;

/**
 * This class holds the configuration of the launcher.
 */
public class Installation implements LauncherRunContext {

    /** The map with the framework properties. */
    private final Map<String, String> fwkProperties = new HashMap<>();

    /** Bundle map */
    private final Map<Integer, List<URL>> bundleMap = new HashMap<>();

    /** Artifacts to be installed */
    private final List<URL> installables = new ArrayList<>();

    /** Configurations, they are installed on first start. */
    private final List<Object[]> configurations = new ArrayList<>();

    /** The list of app jars. */
    private final List<URL> appJars = new ArrayList<>();

    private volatile Logger logger;

    /**
     * Add an application jar.
     * @param jar The application jar
     */
    public void addAppJar(final URL jar) {
        this.appJars.add(jar);
    }

    /**
     * Get the list of application jars.
     * @return The list of app jars
     */
    public List<URL> getAppJars() {
        return this.appJars;
    }

    /**
     * Add a bundle with the given start level
     * @param startLevel The start level
     * @param file The url to the bundle file
     */
    public void addBundle(final Integer startLevel, final URL file) {
        List<URL> files = bundleMap.get(startLevel);
        if (files == null) {
            files = new ArrayList<>();
            bundleMap.put(startLevel, files);
        }
        files.add(file);
    }

    /**
     * Add an artifact to be installed by the installer
     * @param file The url to the file
     */
    public void addInstallableArtifact(final URL file) {
        this.installables.add(file);
    }

    /**
     * Add a configuration
     * @param pid The pid
     * @param factoryPid The factory pid
     * @param properties The propertis
     */
    public void addConfiguration(
            final String pid, final String factoryPid, final Dictionary<String, Object> properties) {
        this.configurations.add(new Object[] {pid, factoryPid, properties});
    }

    /**
     * @see org.apache.sling.feature.launcher.spi.LauncherRunContext#getFrameworkProperties()
     */
    @Override
    public Map<String, String> getFrameworkProperties() {
        return this.fwkProperties;
    }

    public void addFrameworkProperty(String key, String value) {
        this.fwkProperties.put(key, value);
    }

    /**
     * @see org.apache.sling.feature.launcher.spi.LauncherRunContext#getBundleMap()
     */
    @Override
    public Map<Integer, List<URL>> getBundleMap() {
        return this.bundleMap;
    }

    /**
     * @see org.apache.sling.feature.launcher.spi.LauncherRunContext#getConfigurations()
     */
    @Override
    public List<Object[]> getConfigurations() {
        return this.configurations;
    }

    /**
     * @see org.apache.sling.feature.launcher.spi.LauncherRunContext#getInstallableArtifacts()
     */
    @Override
    public List<URL> getInstallableArtifacts() {
        return this.installables;
    }

    /**
     * Clear all in-memory objects
     */
    public void clear() {
        this.configurations.clear();
        this.fwkProperties.clear();
        this.bundleMap.clear();
        this.installables.clear();
    }

    public void setLogger(final Logger l) {
        this.logger = l;
    }

    @Override
    public Logger getLogger() {
        return this.logger;
    }
}
