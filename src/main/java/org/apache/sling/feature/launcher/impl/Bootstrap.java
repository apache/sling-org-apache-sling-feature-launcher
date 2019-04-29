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
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.io.file.ArtifactHandler;
import org.apache.sling.feature.io.file.ArtifactManager;
import org.apache.sling.feature.io.json.FeatureJSONWriter;
import org.apache.sling.feature.launcher.impl.launchers.FrameworkLauncher;
import org.apache.sling.feature.launcher.spi.Launcher;
import org.apache.sling.feature.launcher.spi.LauncherPrepareContext;
import org.osgi.framework.FrameworkEvent;
import org.slf4j.Logger;

/**
 * This is the bootstrap class. ´ß
 */
public class Bootstrap {

    private final LauncherConfig config;

    private final Logger logger;

    public Bootstrap(final LauncherConfig config, final Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    /**
     * Get an artifact id for the Apache Felix framework
     *
     * @param version The version to use or {@code null} for the default version
     * @return The artifact id
     * @throws IllegalArgumentException If the provided version is invalid
     */
    private ArtifactId getFelixFrameworkId(final String version) {
        return new ArtifactId("org.apache.felix", "org.apache.felix.framework", version != null ? version : "6.0.1",
                null, null);
    }


    private void prepare() {
        this.config.getVariables().put("sling.home", this.config.getHomeDirectory().getAbsolutePath());
        if (this.config.getVariables().get("repository.home") == null) {
            this.config.getVariables().put("repository.home",
                    this.config.getHomeDirectory().getAbsolutePath() + File.separatorChar + "repository");
        }
        this.config.getVariables().put("sling.launchpad",
                this.config.getHomeDirectory().getAbsolutePath() + "/launchpad");

        final Installation installation = this.config.getInstallation();

        // set sling home, and use separate locations for launchpad and properties
        installation.getFrameworkProperties().put("sling.home", this.config.getHomeDirectory().getAbsolutePath());
        installation.getFrameworkProperties().put("sling.launchpad",
                this.config.getHomeDirectory().getAbsolutePath() + "/launchpad");
        if (!installation.getFrameworkProperties().containsKey("repository.home")) {
            installation.getFrameworkProperties().put("repository.home",
                    this.config.getHomeDirectory().getAbsolutePath() + File.separatorChar + "repository");
        }
        installation.getFrameworkProperties().put("sling.properties", "conf/sling.properties");
        installation.getFrameworkProperties().put("sling.feature",
                getApplicationFeatureFile(this.config).toURI().toString());


        // additional OSGi properties
        // move storage inside launcher
        if ( installation.getFrameworkProperties().get(STORAGE_PROPERTY) == null ) {
            installation.getFrameworkProperties().put(STORAGE_PROPERTY,
                    this.config.getHomeDirectory().getAbsolutePath() + File.separatorChar + "framework");
        }
        // set start level to 30
        if ( installation.getFrameworkProperties().get(START_LEVEL_PROP) == null ) {
            installation.getFrameworkProperties().put(START_LEVEL_PROP, "30");
        }
    }

    public void run() {
        this.logger.info("");
        this.logger.info("Apache Sling Application Launcher");
        this.logger.info("---------------------------------");


        this.logger.info("Initializing...");
        prepare();

        final Launcher launcher = new FrameworkLauncher();

        try (ArtifactManager artifactManager = ArtifactManager.getArtifactManager(this.config)) {

            this.logger.info("Artifact Repositories: {}", Arrays.toString(this.config.getRepositoryUrls()));
            this.logger.info("Assembling provisioning model...");

            try {
                boolean restart = this.config.getFeatureFiles().length == 0;

                Map<ArtifactId, Feature> loadedFeatures = new HashMap<>();
                final Feature app = assemble(artifactManager, loadedFeatures);

                this.logger.info("");
                this.logger.info("Assembling launcher...");

                final LauncherPrepareContext ctx = new LauncherPrepareContext()
                {
                    @Override
                    public File getArtifactFile(final ArtifactId artifact) throws IOException
                    {
                        final ArtifactHandler handler = artifactManager.getArtifactHandler(":" + artifact.toMvnPath());
                        return handler.getFile();
                    }

                    @Override
                    public void addAppJar(final File jar)
                    {
                        config.getInstallation().addAppJar(jar);
                    }
                };

                launcher.prepare(ctx, getFelixFrameworkId(this.config.getFrameworkVersion()), app);

                FeatureProcessor.prepareLauncher(ctx, this.config, app, loadedFeatures);

                this.logger.info("Using {} local artifacts, {} cached artifacts, and {} downloaded artifacts",
                        this.config.getLocalArtifacts(), this.config.getCachedArtifacts(),
                        this.config.getDownloadedArtifacts());

                if (restart) {
                    this.config.getInstallation().getInstallableArtifacts().clear();
                    this.config.getInstallation().getConfigurations().clear();
                    this.config.getInstallation().getBundleMap().clear();
                }
            } catch ( final Exception iae) {
                this.logger.error("Error while assembling launcher: {}", iae.getMessage(), iae);
                System.exit(1);
            }
        }
        catch (IOException ex) {
            this.logger.error("Unable to setup artifact manager: {}", ex.getMessage(), ex);
            System.exit(1);
        }

        try {
            run(launcher);
        } catch ( final Exception iae) {
            this.logger.error("Error while running launcher: {}", iae.getMessage(), iae);
            System.exit(1);
        }
    }

    private Feature assemble(final ArtifactManager artifactManager,
            Map<ArtifactId, Feature> loadedFeatures) throws IOException
    {
        if (this.config.getFeatureFiles().length == 0) {
            File application = getApplicationFeatureFile(this.config);
            if (application.isFile()) {
                this.config.addFeatureFiles(application.toURI().toURL().toString());
            }
            else {
                throw new IllegalStateException("No feature(s) to launch found and none where specified");
            }
            return FeatureProcessor.createApplication(this.config, artifactManager, loadedFeatures);
        }
        else
        {
            final Feature app = FeatureProcessor.createApplication(this.config, artifactManager, loadedFeatures);

            // write application back
            final File file = getApplicationFeatureFile(this.config);
            file.getParentFile().mkdirs();

            try (final FileWriter writer = new FileWriter(file))
            {
                FeatureJSONWriter.write(writer, app);
            }
            catch (final IOException ioe)
            {
                this.logger.error("Error while writing application file: {}", ioe.getMessage(), ioe);
                System.exit(1);
            }
            return app;
        }
    }

    private static File getApplicationFeatureFile(final LauncherConfig launcherConfig) {
        return new File(launcherConfig.getHomeDirectory(), "resources" + File.separatorChar + "provisioning" + File.separatorChar + "application.json");
    }

    private static final String STORAGE_PROPERTY = "org.osgi.framework.storage";

    private static final String START_LEVEL_PROP = "org.osgi.framework.startlevel.beginning";

    /**
     * Run launcher.
     * @throws Exception If anything goes wrong
     */
    private void run(final Launcher launcher) throws Exception {
        this.logger.info("");
        this.logger.info("Starting launcher...");
        this.logger.info("Launcher Home: {}", config.getHomeDirectory().getAbsolutePath());
        this.logger.info("Cache Directory: {}", config.getCacheDirectory().getAbsolutePath());
        this.logger.info("");

        final Installation installation = config.getInstallation();

        // set sling home, and use separate locations for launchpad and properties
        installation.getFrameworkProperties().put("sling.home", config.getHomeDirectory().getAbsolutePath());
        installation.getFrameworkProperties().put("sling.launchpad", config.getHomeDirectory().getAbsolutePath() + "/launchpad");
        if (!installation.getFrameworkProperties().containsKey("repository.home")) {
            installation.getFrameworkProperties().put("repository.home", config.getHomeDirectory().getAbsolutePath() + File.separatorChar + "repository");
        }
        installation.getFrameworkProperties().put("sling.properties", "conf/sling.properties");
        installation.getFrameworkProperties().put("sling.feature", getApplicationFeatureFile(config).toURI().toString());


        // additional OSGi properties
        // move storage inside launcher
        if ( installation.getFrameworkProperties().get(STORAGE_PROPERTY) == null ) {
            installation.getFrameworkProperties().put(STORAGE_PROPERTY, config.getHomeDirectory().getAbsolutePath() + File.separatorChar + "framework");
        }
        // set start level to 30
        if ( installation.getFrameworkProperties().get(START_LEVEL_PROP) == null ) {
            installation.getFrameworkProperties().put(START_LEVEL_PROP, "30");
        }

        while (launcher.run(installation, createClassLoader(installation)) == FrameworkEvent.STOPPED_SYSTEM_REFRESHED) {
            this.logger.info("Framework restart due to extension refresh");
        }
    }

    /**
     * Create the class loader.
     * @param installation The launcher configuration
     * @return The classloader.
     * @throws Exception If anything goes wrong
     */
    public ClassLoader createClassLoader(final Installation installation) throws Exception {
        final List<URL> list = new ArrayList<>();
        for(final File f : installation.getAppJars()) {
            try {
                list.add(f.toURI().toURL());
            } catch (IOException e) {
                // ignore
            }
        }
        list.add(Bootstrap.class.getProtectionDomain().getCodeSource().getLocation());

        final URL[] urls = list.toArray(new URL[list.size()]);

        if (this.logger.isDebugEnabled()) {
            this.logger.debug("App classpath: ");
            for (int i = 0; i < urls.length; i++) {
                this.logger.debug(" - {}", urls[i]);
            }
        }

        // create a paranoid class loader, loading from parent last
        final ClassLoader cl = new URLClassLoader(urls) {
            @Override
            public final Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                // First check if it's already loaded
                Class<?> clazz = findLoadedClass(name);

                if (clazz == null) {

                    try {
                        clazz = findClass(name);
                    } catch (ClassNotFoundException cnfe) {
                        ClassLoader parent = getParent();
                        if (parent != null) {
                            // Ask to parent ClassLoader (can also throw a CNFE).
                            clazz = parent.loadClass(name);
                        } else {
                            // Propagate exception
                            throw cnfe;
                        }
                    }
                }

                if (resolve) {
                    resolveClass(clazz);
                }

                return clazz;
            }

            @Override
            public final URL getResource(final String name) {

                URL resource = findResource(name);
                ClassLoader parent = this.getParent();
                if (resource == null && parent != null) {
                    resource = parent.getResource(name);
                }

                return resource;
            }
        };

        Thread.currentThread().setContextClassLoader(cl);

        return cl;
    }
}
