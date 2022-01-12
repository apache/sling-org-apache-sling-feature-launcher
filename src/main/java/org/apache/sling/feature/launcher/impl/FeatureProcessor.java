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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
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
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.builder.ArtifactProvider;
import org.apache.sling.feature.builder.BuilderContext;
import org.apache.sling.feature.builder.FeatureBuilder;
import org.apache.sling.feature.builder.MergeHandler;
import org.apache.sling.feature.builder.PostProcessHandler;
import org.apache.sling.feature.io.IOUtils;
import org.apache.sling.feature.io.archive.ArchiveReader;
import org.apache.sling.feature.io.artifacts.ArtifactHandler;
import org.apache.sling.feature.io.artifacts.ArtifactManager;
import org.apache.sling.feature.io.json.FeatureJSONReader;
import org.apache.sling.feature.launcher.spi.LauncherPrepareContext;
import org.apache.sling.feature.launcher.spi.extensions.ExtensionHandler;
import org.slf4j.Logger;

public class FeatureProcessor {

    /**
     * Initialize the launcher
     * Read the features and prepare the application
     * @param config The current configuration
     * @param artifactManager The artifact manager
     * @param loadedFeatures This map will be populated with features that were loaded as part of this process
     * @return The merged feature representing the application
     * @throws IOException when an IO exception occurs during application creation
     */
    public static Feature createApplication(final Logger logger, final LauncherConfig config,
            final ArtifactManager artifactManager, final Map<ArtifactId, Feature> loadedFeatures) throws IOException
    {
        final BuilderContext builderContext = new BuilderContext(id -> {
            try {
                final ArtifactHandler handler = artifactManager.getArtifactHandler(id.toMvnUrl());
                try (final Reader r = new InputStreamReader(handler.getLocalURL().openStream(), "UTF-8")) {
                    final Feature f = FeatureJSONReader.read(r, handler.getUrl());
                    return f;
                }
            } catch (IOException e) {
                // ignore
                return null;
            }
        });
        final ArtifactProvider provider = new ArtifactProvider() {

            @Override
            public URL provide(final ArtifactId id) {
                try {
                    final ArtifactHandler handler = artifactManager.getArtifactHandler(id.toMvnUrl());
                    return handler.getLocalURL();
                } catch (final IOException e) {
                    // ignore
                    return null;
                }
            }
        };
        builderContext.setArtifactProvider(provider);

        config.getArtifactClashOverrides().stream().forEach(id -> builderContext.addArtifactsOverride(id));
        builderContext.addConfigsOverrides(config.getConfigClashOverrides());
        builderContext.addVariablesOverrides(config.getVariables());
        builderContext.addFrameworkPropertiesOverrides(config.getInstallation().getFrameworkProperties());
        builderContext.addMergeExtensions(StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                ServiceLoader.load(MergeHandler.class).iterator(), Spliterator.ORDERED), false)
                    .toArray(MergeHandler[]::new));
        builderContext.addPostProcessExtensions(StreamSupport.stream(Spliterators.spliteratorUnknownSize(
            ServiceLoader.load(PostProcessHandler.class).iterator(), Spliterator.ORDERED), false)
                .toArray(PostProcessHandler[]::new));
        for (Map.Entry<String, Map<String,String>> entry : config.getExtensionConfiguration().entrySet()) {
            builderContext.setHandlerConfiguration(entry.getKey(), entry.getValue());
        }

        List<Feature> features = new ArrayList<>();
        final byte[] buffer = new byte[1024*1024*256];
        for (final String featureFile : config.getFeatureFiles()) {
            for (final String initFile : IOUtils.getFeatureFiles(config.getHomeDirectory(), featureFile)) {
                if ( initFile.endsWith(IOUtils.EXTENSION_FEATURE_ARCHIVE) ) {
                    logger.debug("Reading feature archive {}", initFile);
                    final ArtifactHandler featureArtifact = artifactManager.getArtifactHandler(initFile);
                    try (final InputStream is = featureArtifact.getLocalURL().openStream()) {
                        for(final Feature feature : ArchiveReader.read(is, (id, stream) -> {

                                if ( provider.provide(id) == null ) {
                                    final File artifactFile = new File(config.getCacheDirectory(),
                                            id.toMvnPath().replace('/', File.separatorChar));
                                    if (!artifactFile.exists()) {
                                        artifactFile.getParentFile().mkdirs();
                                        try (final OutputStream os = new FileOutputStream(artifactFile)) {
                                            int l = 0;
                                            while ((l = stream.read(buffer)) > 0) {
                                                os.write(buffer, 0, l);
                                            }
                                        }
                                    }
                                }
                            })) {

                            features.add(feature);
                            loadedFeatures.put(feature.getId(), feature);
                        }
                    } catch (final IOException ioe) {
                        logger.info("Unable to read feature archive from " + initFile, ioe);
                    }
                } else {
                    logger.debug("Reading feature file {}", initFile);
                    final ArtifactHandler featureArtifact = artifactManager.getArtifactHandler(initFile);
                    try (final Reader r = new InputStreamReader(featureArtifact.getLocalURL().openStream(), "UTF-8")) {
                        final Feature f = FeatureJSONReader.read(r, featureArtifact.getUrl());
                        loadedFeatures.put(f.getId(), f);
                        features.add(f);
                    } catch (Exception ex) {
                        throw new IOException("Error reading feature: " + initFile, ex);
                    }
                }
            }
        }

        final Feature app = FeatureBuilder.assemble(config.getLaunchFeatureId(), builderContext, features.toArray(new Feature[0]));
        loadedFeatures.put(app.getId(), app);

        FeatureBuilder.resolveVariables(app, config.getVariables());

        return app;
    }

    /**
     * Prepare the launcher
     * - add all bundles to the bundle map of the installation object
     * - add all other artifacts to the install directory (only if startup mode is INSTALL)
     * - process configurations
     * @param ctx The launcher prepare context
     * @param config The launcher configuration
     * @param app The merged feature to launch
     * @param loadedFeatures The features previously loaded by the launcher, this includes features that
     * were passed in via file:// URLs from the commandline
     * @throws Exception when something goes wrong
     */
    public static void prepareLauncher(final LauncherPrepareContext ctx, final LauncherConfig config,
            final Feature app, Map<ArtifactId, Feature> loadedFeatures) throws Exception {
        for(final Map.Entry<Integer, List<Artifact>> entry : app.getBundles().getBundlesByStartOrder().entrySet()) {
            for(final Artifact a : entry.getValue()) {
                final URL artifactFile = ctx.getArtifactFile(a.getId());

                // add URL to feature metadata
                a.getMetadata().put(URL.class.getName(), artifactFile.toString());
                config.getInstallation().addBundle(entry.getKey(), artifactFile);
            }
        }

        for (final Configuration cfg : app.getConfigurations()) {
            if (Configuration.isFactoryConfiguration(cfg.getPid())) {
                config.getInstallation().addConfiguration(Configuration.getName(cfg.getPid()),
                        Configuration.getFactoryPid(cfg.getPid()), cfg.getConfigurationProperties());
            } else {
                config.getInstallation().addConfiguration(cfg.getPid(), null, cfg.getConfigurationProperties());
            }
        }

        for (final Map.Entry<String, String> prop : app.getFrameworkProperties().entrySet()) {
            if ( !config.getInstallation().getFrameworkProperties().containsKey(prop.getKey()) ) {
                config.getInstallation().getFrameworkProperties().put(prop.getKey(), prop.getValue());
            }
        }

        extensions: for(final Extension ext : app.getExtensions()) {
            for (ExtensionHandler handler : ServiceLoader.load(ExtensionHandler.class,  FeatureProcessor.class.getClassLoader()))
            {
                if (handler.handle(new ExtensionContextImpl(ctx, config.getInstallation(), loadedFeatures), ext)) {
                    continue extensions;
                }
            }
            if ( ext.getState() == ExtensionState.REQUIRED ) {
                throw new Exception("Unknown required extension " + ext.getName());
            }
        }
    }
}
