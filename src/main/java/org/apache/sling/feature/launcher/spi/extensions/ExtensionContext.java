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
package org.apache.sling.feature.launcher.spi.extensions;

import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.launcher.spi.LauncherPrepareContext;
import org.apache.sling.feature.launcher.spi.LauncherRunContext;

/**
 * This context object is provided to launcher extensions.
 */
public interface ExtensionContext extends LauncherPrepareContext, LauncherRunContext {
    /**
     * Add a bundle to be installed by the launcher.
     * @param startLevel The start level for the bundle.
     * @param file The file with the bundle.
     */
    public void addBundle(final Integer startLevel, final URL file);

    /**
     * Add an artifact to be installed by the launcher
     * @param file The file
     */
    public void addInstallableArtifact(final URL file);

    /**
     * Add a configuration to be installed by the launcher
     * @param pid The pid
     * @param factoryPid The factory pid
     * @param properties The propertis
     */
    public void addConfiguration(
            final String pid, final String factoryPid, final Dictionary<String, Object> properties);

    /**
     * Add a framework property to be set by the launcher.
     * @param key The key for the property.
     * @param value The value for the property.
     */
    public void addFrameworkProperty(final String key, final String value);

    /**
     * Return the feature object for a given Artifact ID. It looks for the requested feature
     * in the list of features provided from the launcher commandline as well as in the configured
     * repositories.
     * @param artifact The artifact ID for the feature.
     * @return The Feature Model or null if the artifact cannot be found.
     * @throws IOException If the artifact can be found, but creating a Feature
     * Model out of it causes an exception.
     */
    Feature getFeature(ArtifactId artifact) throws IOException;
}
