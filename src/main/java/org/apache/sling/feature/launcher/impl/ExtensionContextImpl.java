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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.io.json.FeatureJSONReader;
import org.apache.sling.feature.launcher.spi.LauncherPrepareContext;
import org.apache.sling.feature.launcher.spi.extensions.ExtensionContext;
import org.slf4j.Logger;

class ExtensionContextImpl implements ExtensionContext {

    private final Installation installation;
    private final LauncherPrepareContext prepareContext;
    private final Map<ArtifactId, Feature> loadedFeatures;

    ExtensionContextImpl(LauncherPrepareContext lpc, Installation inst, Map<ArtifactId, Feature> featureMap) {
        prepareContext = lpc;
        installation = inst;
        loadedFeatures = featureMap;
    }

    @Override
    public Logger getLogger() {
        return prepareContext.getLogger();
    }

    @Override
    public void addBundle(Integer startLevel, URL file) {
        installation.addBundle(startLevel, file);
    }

    @Override
    public void addInstallableArtifact(URL file) {
        installation.addInstallableArtifact(file);
    }

    @Override
    public void addConfiguration(String pid, String factoryPid, Dictionary<String, Object> properties) {
        installation.addConfiguration(pid, factoryPid, properties);
    }

    @Override
    public void addFrameworkProperty(String key, String value) {
        installation.addFrameworkProperty(key, value);
    }

    @Override
    public Map<String, String> getFrameworkProperties() {
        return installation.getFrameworkProperties();
    }

    @Override
    public Map<Integer, List<URL>> getBundleMap() {
        return installation.getBundleMap();
    }

    @Override
    public List<Object[]> getConfigurations() {
        return installation.getConfigurations();
    }

    @Override
    public List<URL> getInstallableArtifacts() {
        return installation.getInstallableArtifacts();
    }

    @Override
    public void addAppJar(URL jar) {
        prepareContext.addAppJar(jar);
    }

    @Override
    public URL getArtifactFile(ArtifactId artifact) throws IOException {
        return prepareContext.getArtifactFile(artifact);
    }

    @Override
    public Feature getFeature(ArtifactId artifact) throws IOException {
        Feature f = loadedFeatures.get(artifact);
        if (f != null) return f;

        URL file = getArtifactFile(artifact);
        if (file == null) return null;

        try (Reader r = new InputStreamReader(file.openStream(), "UTF-8")) {
            return FeatureJSONReader.read(r, artifact.toMvnUrl());
        }
    }
}
