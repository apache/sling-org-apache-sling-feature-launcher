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
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.FeatureConstants;
import org.apache.sling.feature.builder.BuilderContext;
import org.apache.sling.feature.builder.FeatureBuilder;
import org.apache.sling.feature.builder.FeatureExtensionHandler;
import org.apache.sling.feature.io.ArtifactHandler;
import org.apache.sling.feature.io.ArtifactManager;
import org.apache.sling.feature.io.IOUtils;
import org.apache.sling.feature.io.json.FeatureJSONReader;
import org.apache.sling.feature.launcher.spi.LauncherPrepareContext;
import org.apache.sling.feature.launcher.spi.extensions.ExtensionHandler;

public class FeatureProcessor {

    /**
     * Initialize the launcher
     * Read the features and prepare the application
     * @param config The current configuration
     * @param artifactManager The artifact manager
     */
    public static Feature createApplication(final LauncherConfig config,
            final ArtifactManager artifactManager) throws IOException
    {

        final BuilderContext builderContext = new BuilderContext(
            id -> {
                try {
                    final ArtifactHandler handler = artifactManager.getArtifactHandler(id.toMvnUrl());
                    try (final FileReader r = new FileReader(handler.getFile())) {
                        final Feature f = FeatureJSONReader.read(r, handler.getUrl());
                        return f;
                    }

                } catch (final IOException e) {
                    // ignore
                }
                return null;
            }).add(StreamSupport.stream(Spliterators.spliteratorUnknownSize(
            ServiceLoader.load(FeatureExtensionHandler.class).iterator(), Spliterator.ORDERED), false).toArray(FeatureExtensionHandler[]::new));

        List<Feature> features = new ArrayList<>();

        for (final String initFile : config.getFeatureFiles())
        {
            try
            {
                final Feature f = IOUtils.getFeature(initFile, artifactManager);
                features.add(f);
            }
            catch (Exception ex)
            {
                throw new IOException("Error reading feature: " + initFile, ex);
            }
        }

        Collections.sort(features);

        // TODO make feature id configurable
        final Feature app = FeatureBuilder.assemble(ArtifactId.fromMvnId("group:assembled:1.0.0"), builderContext, features.toArray(new Feature[0]));

        final Artifact a = new Artifact(ArtifactId.parse("org.apache.sling/org.apache.sling.launchpad.api/1.2.0"));
        a.getMetadata().put(org.apache.sling.feature.Artifact.KEY_START_ORDER, "1");
        app.getBundles().add(a);

        // TODO: this sucks
        for (Artifact bundle : app.getBundles()) {
            if ( bundle.getStartOrder() == 0) {
                final int so = bundle.getMetadata().get("start-level") != null ? Integer.parseInt(bundle.getMetadata().get("start-level")) : 1;
                bundle.setStartOrder(so);
            }
        }

        FeatureBuilder.resolveVariables(app, config.getVariables());

        return app;
    }

    /**
     * Prepare the launcher
     * - add all bundles to the bundle map of the installation object
     * - add all other artifacts to the install directory (only if startup mode is INSTALL)
     * - process configurations
     */
    public static void prepareLauncher(final LauncherPrepareContext ctx, final LauncherConfig config,
            final Feature app) throws Exception {
        for(final Map.Entry<Integer, List<Artifact>> entry : app.getBundles().getBundlesByStartOrder().entrySet()) {
            for(final Artifact a : entry.getValue()) {
                final File artifactFile = ctx.getArtifactFile(a.getId());

                config.getInstallation().addBundle(entry.getKey(), artifactFile);
            }
        }
        extensions: for(final Extension ext : app.getExtensions()) {
            for (ExtensionHandler handler : ServiceLoader.load(ExtensionHandler.class,  FeatureProcessor.class.getClassLoader()))
            {
                if (handler.handle(ext, ctx, config.getInstallation())) {
                    continue extensions;
                }
            }
            if ( ext.isRequired() ) {
                throw new Exception("Unknown required extension " + ext.getName());
            }
        }

        for (final Configuration cfg : app.getConfigurations()) {
            if ( cfg.isFactoryConfiguration() ) {
                config.getInstallation().addConfiguration(cfg.getName(), cfg.getFactoryPid(), cfg.getProperties());
            } else {
                config.getInstallation().addConfiguration(cfg.getPid(), null, cfg.getProperties());
            }
        }

        for (final Map.Entry<String, String> prop : app.getFrameworkProperties()) {
            if ( !config.getInstallation().getFrameworkProperties().containsKey(prop.getKey()) ) {
                config.getInstallation().getFrameworkProperties().put(prop.getKey(), prop.getValue());
            }
        }
    }

    /**
     * Prepare the cache
     * - add all bundles
     * - add all other artifacts (only if startup mode is INSTALL)
     */
    public static Map<Artifact, File> calculateArtifacts(final ArtifactManager artifactManager,
        final Feature app) throws Exception
    {
        Map<Artifact, File> result = new HashMap<>();
        for (final Map.Entry<Integer, List<Artifact>> entry : app.getBundles().getBundlesByStartOrder().entrySet())
        {
            for (final Artifact a : entry.getValue())
            {
                final ArtifactHandler handler = artifactManager.getArtifactHandler(":" + a.getId().toMvnPath());
                final File artifactFile = handler.getFile();

                result.put(a, artifactFile);
            }
        }
        for (final Extension ext : app.getExtensions())
        {
            if (ext.getType() == ExtensionType.ARTIFACTS && ext.getName().equals(FeatureConstants.EXTENSION_NAME_CONTENT_PACKAGES))
            {
                for (final Artifact a : ext.getArtifacts())
                {
                    final ArtifactHandler handler = artifactManager.getArtifactHandler(":" + a.getId().toMvnPath());
                    result.put(a, handler.getFile());
                }
            }
        }
        return result;
    }
}
