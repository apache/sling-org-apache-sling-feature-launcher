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
package org.apache.sling.feature.launcher.impl.launchers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common functionality for the framework start.
 */
public abstract class AbstractRunner implements Callable<Integer> {

    private volatile ServiceTracker<Object, Object> configAdminTracker;

    private volatile ServiceTracker<Object, Object> installerTracker;

    private final List<Object[]> configurations;

    private final List<File> installables;

    protected final Logger logger;

    public AbstractRunner(final List<Object[]> configurations, final List<File> installables) {
        this.configurations = new ArrayList<>(configurations);
        this.installables = installables;
        logger = LoggerFactory.getLogger("launcher");
    }

    protected void setupFramework(final Framework framework, final Map<Integer, List<File>> bundlesMap)
    throws BundleException {
        if ( !configurations.isEmpty() ) {
            this.configAdminTracker = new ServiceTracker<>(framework.getBundleContext(),
                    "org.osgi.service.cm.ConfigurationAdmin",
                    new ServiceTrackerCustomizer<Object, Object>() {

                        @Override
                        public Object addingService(final ServiceReference<Object> reference) {
                            // get config admin
                            final Object cm = framework.getBundleContext().getService(reference);
                            if ( cm != null ) {
                                try {
                                    configure(cm);
                                } finally {
                                    framework.getBundleContext().ungetService(reference);
                                }
                            }
                            return null;
                        }

                        @Override
                        public void modifiedService(ServiceReference<Object> reference, Object service) {
                            // nothing to do
                        }

                        @Override
                        public void removedService(ServiceReference<Object> reference, Object service) {
                            // nothing to do
                        }
            });
            this.configAdminTracker.open();
        }
        if ( !installables.isEmpty() ) {
            this.installerTracker = new ServiceTracker<>(framework.getBundleContext(),
                    "org.apache.sling.installer.api.OsgiInstaller",
                    new ServiceTrackerCustomizer<Object, Object>() {

                        @Override
                        public Object addingService(final ServiceReference<Object> reference) {
                            // get installer
                            final Object installer = framework.getBundleContext().getService(reference);
                            if ( installer != null ) {
                                try {
                                    install(installer);
                                } finally {
                                    framework.getBundleContext().ungetService(reference);
                                }
                            }
                            return null;
                        }

                        @Override
                        public void modifiedService(ServiceReference<Object> reference, Object service) {
                            // nothing to do
                        }

                        @Override
                        public void removedService(ServiceReference<Object> reference, Object service) {
                            // nothing to do
                        }
            });
            this.installerTracker.open();
        }

        try {
            this.install(framework, bundlesMap);
        } catch ( final IOException ioe) {
            throw new BundleException("Unable to install bundles.", ioe);
        }
    }

    protected boolean startFramework(final Framework framework, long timeout, TimeUnit unit) throws BundleException, InterruptedException
    {
        Executor executor = Executors.newSingleThreadExecutor();
        Future<Void> result = ((ExecutorService) executor).submit(new Callable<Void>()
        {
            @Override
            public Void call() throws Exception
            {
                framework.start();
                checkResultingState(framework);
                return null;
            }
        });

        try {
            result.get(timeout, unit);
            return true;
        } catch (TimeoutException ex) {
            return false;
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof BundleException) {
                throw (BundleException) cause;
            } else {
                throw (RuntimeException) cause;
            }
        }
        finally {
            ((ExecutorService) executor).shutdownNow();
        }
    }

    private void configure(final Object configAdmin) {
        try {
            final Method createConfig = configAdmin.getClass().getDeclaredMethod("getConfiguration", String.class, String.class);
            final Method createFactoryConfig = configAdmin.getClass().getDeclaredMethod("getFactoryConfiguration", String.class, String.class, String.class);

            Method updateMethod = null;
            for(final Object[] obj : this.configurations) {
                final Object cfg;
                if ( obj[1] != null ) {
                    cfg = createFactoryConfig.invoke(configAdmin, obj[1], obj[0], null);
                } else {
                    cfg = createConfig.invoke(configAdmin, obj[0], null);
                }
                if ( updateMethod == null ) {
                    updateMethod = cfg.getClass().getDeclaredMethod("update", Dictionary.class);
                }
                updateMethod.invoke(cfg, obj[2]);
            }
        } catch ( final Exception e) {
            logger.error("Unable to create configurations", e);
            throw new RuntimeException(e);
        }
        final Thread t = new Thread(() -> { configAdminTracker.close(); configAdminTracker = null; });
        t.setDaemon(false);
        t.start();
        this.configurations.clear();
    }

    private boolean isSystemBundleFragment(final Bundle installedBundle) {
        final String fragmentHeader = getFragmentHostHeader(installedBundle);
        return fragmentHeader != null
            && fragmentHeader.indexOf(Constants.EXTENSION_DIRECTIVE) > 0;
    }

    /**
     * Gets the bundle's Fragment-Host header.
     */
    private String getFragmentHostHeader(final Bundle b) {
        return b.getHeaders().get( Constants.FRAGMENT_HOST );
    }

    /**
     * Install the bundles
     * @param bundleMap The map with the bundles indexed by start level
     * @throws IOException, BundleException If anything goes wrong.
     */
    private void install(final Framework framework, final Map<Integer, List<File>> bundleMap)
    throws IOException, BundleException {
        final BundleContext bc = framework.getBundleContext();
        int defaultStartLevel = getProperty(bc, "felix.startlevel.bundle", 1);
        for(final Integer startLevel : sortStartLevels(bundleMap.keySet(), defaultStartLevel)) {
            logger.debug("Installing bundles with start level {}", startLevel);

            for(final File file : bundleMap.get(startLevel)) {
                logger.debug("- {}", file.getName());

                // use reference protocol. This avoids copying the binary to the cache directory
                // of the framework
                final Bundle bundle = bc.installBundle("reference:" + file.toURI().toURL(), null);

                // fragment?
                if ( !isSystemBundleFragment(bundle) && getFragmentHostHeader(bundle) == null ) {
                    if ( startLevel > 0 ) {
                        bundle.adapt(BundleStartLevel.class).setStartLevel(startLevel);
                    }
                    bundle.start();
                }
            }
        }
    }

    /**
     * Sort the start levels in the ascending order. The only exception is the start level
     * "0", which should be put at the position configured in {@code felix.startlevel.bundle}.
     *
     * @param startLevels integer start levels
     * @return sorted start levels
     */
    private static Iterable<Integer> sortStartLevels(final Collection<Integer> startLevels, final int defaultStartLevel) {
        final List<Integer> result = new ArrayList<>(startLevels);
        Collections.sort(result, (o1, o2) -> {
            int i1 = o1 == 0 ? defaultStartLevel : o1;
            int i2 = o2 == 0 ? defaultStartLevel : o2;
            return Integer.compare(i1, i2);
        });
        return result;
    }

    private static int getProperty(BundleContext bc, String propName, int defaultValue) {
        String val = bc.getProperty(propName);
        if (val == null) {
            return defaultValue;
        } else {
            return Integer.parseInt(val);
        }
    }

    private void install(final Object installer) {
        try {
            final Class<?> installableResourceClass = installer.getClass().getClassLoader().loadClass("org.apache.sling.installer.api.InstallableResource");
            final Object resources = Array.newInstance(installableResourceClass, this.installables.size());
            final Method registerResources = installer.getClass().getDeclaredMethod("registerResources", String.class, resources.getClass());
            final Constructor<?> constructor = installableResourceClass.getDeclaredConstructor(String.class,
                    InputStream.class,
                    Dictionary.class,
                    String.class,
                    String.class,
                    Integer.class);

            for(int i=0; i<this.installables.size();i++) {
                final File f = this.installables.get(i);
                final Dictionary<String, Object> dict = new Hashtable<>();
                dict.put("resource.uri.hint", f.toURI().toString());
                final Object rsrc = constructor.newInstance(f.getAbsolutePath(),
                        new FileInputStream(f),
                        dict,
                        f.getName(),
                        "file",
                        null);
                Array.set(resources, i, rsrc);
            }
            registerResources.invoke(installer, "cloudlauncher", resources);
        } catch ( final Exception e) {
            logger.error("Unable to contact installer and install additional artifacts", e);
            throw new RuntimeException(e);
        } finally  {
            final Thread t = new Thread(() -> {
                installerTracker.close();
                installerTracker = null;
            });
            t.setDaemon(false);
            t.start();
            this.installables.clear();
        }
    }

    static void checkResultingState(final Framework framework) {
        if (!Boolean.TRUE.toString().equals(framework.getBundleContext().
                getProperty("sling.feature.launcher.failonerror")))
            return; // if the failonerror property is not set, don't check

        // Check that all bundles were resolved and started successfully.
        for (Bundle b : framework.getBundleContext().getBundles()) {
            if ((b.adapt(BundleRevision.class).getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {
                // It's a fragment, these don't start
                continue;
            }

            if (b.getState() != Bundle.ACTIVE) {
                throw new IllegalStateException("Was not able to start bundle: " + b + " See the log for further details.");
            }
        }
    }
}
