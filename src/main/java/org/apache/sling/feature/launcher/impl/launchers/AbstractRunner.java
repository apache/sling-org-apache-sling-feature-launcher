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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
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
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common functionality for the framework start.
 */
public abstract class AbstractRunner implements Callable<Integer> {

    /**
     * Configuration property for Apache Felix Configuration Admin persistence
     * manager.
     */
    private static final String CM_CONFIG_PM = "felix.cm.pm";

    /** Name of the feature launcher persistence manager. */
    private static final String PM_FEATURE_LAUNCHER = "featurelauncher";

    /** Filter expression to get the memory persistence manager. */
    private static final String PM_MEMORY_FILTER = "(&(" + Constants.OBJECTCLASS
            + "=org.apache.felix.cm.PersistenceManager)(name=memory))";

    private volatile ServiceTracker<Object, Object> configAdminTracker;

    private volatile ServiceTracker<Object, Object> installerTracker;

    private final List<Object[]> configurations;

    private final List<URL> installables;

    protected final Logger logger;

    private Supplier<String> featureSupplier;

    private BiConsumer<URL, Map<String, String>> bundleReporter;

    public AbstractRunner(final List<Object[]> configurations, final List<URL> installables) {
        this.configurations = new ArrayList<>(configurations);
        this.installables = installables;
        this.logger = LoggerFactory.getLogger("launcher");
    }

    public void setFeatureSupplier(final Supplier<String> supplier) {
        this.featureSupplier = supplier;
    }

    public void setBundleReporter(final BiConsumer<URL, Map<String, String>> reporter) {
        this.bundleReporter = reporter;
    }

    protected void setupFramework(final Framework framework, final Map<Integer, List<URL>> bundlesMap) throws BundleException {
        // check for Apache Felix CM persistence manager config
        final String pm = framework.getBundleContext().getProperty(CM_CONFIG_PM);
        if (PM_FEATURE_LAUNCHER.equals(pm)) {
            logger.info("Using feature launcher configuration admin persistence manager");
            try {
                // we start a tracker for the memory PM
                this.configAdminTracker = new ServiceTracker<>(framework.getBundleContext(),
                        framework.getBundleContext().createFilter(PM_MEMORY_FILTER),

                        new ServiceTrackerCustomizer<Object, Object>() {
                            private volatile ServiceRegistration<?> reg;

                            @Override
                            public Object addingService(final ServiceReference<Object> reference) {
                                // get memory pm
                                final Object memoryPM = framework.getBundleContext().getService(reference);
                                if (memoryPM != null) {
                                    try {
                                        // we re use the memory PM (it is not used anyway)
                                        // and simply store the configs there using reflection
                                        final Method storeMethod = memoryPM.getClass().getDeclaredMethod("store",
                                                String.class, Dictionary.class);
                                        for (final Object[] obj : configurations) {
                                            @SuppressWarnings("unchecked")
                                            final Dictionary<String, Object> props = (Dictionary<String, Object>) obj[2];
                                            final String pid;
                                            if (obj[1] != null) {
                                                final String factoryPid = (String) obj[1];
                                                pid = factoryPid.concat("~").concat((String) obj[0]);
                                                props.put("service.factoryPid", factoryPid);
                                            } else {
                                                pid = (String) obj[0];
                                            }
                                            props.put(Constants.SERVICE_PID, pid);
                                            storeMethod.invoke(memoryPM, pid, props);
                                        }
                                        // register feature launcher pm
                                        final Dictionary<String, Object> properties = new Hashtable<>();
                                        properties.put("name", PM_FEATURE_LAUNCHER);
                                        reg = reference.getBundle().getBundleContext().registerService(
                                                "org.apache.felix.cm.PersistenceManager", memoryPM, properties);
                                    } catch (IllegalAccessException | IllegalArgumentException
                                            | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                                return memoryPM;
                            }

                            @Override
                            public void modifiedService(ServiceReference<Object> reference, Object service) {
                                // nothing to do
                            }

                            @Override
                            public void removedService(ServiceReference<Object> reference, Object service) {
                                if (reg != null) {
                                    reg.unregister();
                                    reg = null;
                                }
                                reference.getBundle().getBundleContext().ungetService(reference);
                            }
                        });
            } catch (final InvalidSyntaxException e) {
                // the filter is constant so this should really not happen
                throw new RuntimeException(e);
            }
            this.configAdminTracker.open(true);
        } else if (!configurations.isEmpty()) {

            this.configAdminTracker = new ServiceTracker<>(framework.getBundleContext(),
                    "org.osgi.service.cm.ConfigurationAdmin", new ServiceTrackerCustomizer<Object, Object>() {

                        @Override
                        public Object addingService(final ServiceReference<Object> reference) {
                            // get config admin
                            final Object cm = framework.getBundleContext().getService(reference);
                            if (cm != null) {
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
            this.configAdminTracker.open(true);
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

        this.install(framework, bundlesMap);
    }

    protected boolean startFramework(final Framework framework, long timeout, TimeUnit unit) throws BundleException, InterruptedException {
        Executor executor = Executors.newSingleThreadExecutor();
        Future<Void> result = ((ExecutorService) executor).submit(new Callable<Void>()
        {
            @Override
            public Void call() throws Exception
            {
                framework.start();
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
    private void install(final Framework framework, final Map<Integer, List<URL>> bundleMap) throws BundleException {
        final BundleContext bc = framework.getBundleContext();
        int defaultStartLevel = getProperty(bc, "felix.startlevel.bundle", 1);
        for(final Integer startLevel : sortStartLevels(bundleMap.keySet(), defaultStartLevel)) {
            logger.debug("Installing bundles with start level {}", startLevel);

            for(final URL file : bundleMap.get(startLevel)) {
                logger.debug("- {}", file);

                // use reference protocol if possible. This avoids copying the binary to the cache directory
                // of the framework
                String location = "";
                if (file.getProtocol().equals("file")) {
                    location = "reference:";
                }
                location = location.concat(file.toString());

                final Bundle bundle = bc.installBundle(location, null);

                // fragment?
                if ( !isSystemBundleFragment(bundle) && getFragmentHostHeader(bundle) == null ) {
                    if ( startLevel > 0 ) {
                        bundle.adapt(BundleStartLevel.class).setStartLevel(startLevel);
                    }
                    bundle.start();
                }

                if ( this.bundleReporter != null ) {
                    final Map<String, String> params = new HashMap<>();
                    params.put(Constants.BUNDLE_SYMBOLICNAME, bundle.getSymbolicName());
                    params.put(Constants.BUNDLE_VERSION, bundle.getVersion().toString());
                    params.put("Bundle-Id", String.valueOf(bundle.getBundleId()));

                    this.bundleReporter.accept(file, params);
                }
                    
            }
        }
    }

    protected void finishStartup(final Framework framework) {
        Bundle featureBundle = null;
        for(final Bundle bundle : framework.getBundleContext().getBundles()) {
            if ( featureSupplier != null && "org.apache.sling.feature".equals(bundle.getSymbolicName()) ) {
                featureBundle = bundle;
            }
        }
        if ( featureBundle != null ) {
            final Bundle bundle = featureBundle;
            // the feature is registered as a prototype to give each client a copy as feature models are mutable
            final Dictionary<String, Object> properties = new Hashtable<>();
            properties.put("name", "org.apache.sling.feature.launcher");
            featureBundle.getBundleContext().registerService(new String[] {"org.apache.sling.feature.Feature"}, 
                new PrototypeServiceFactory<Object>() {

                    @Override
                    public Object getService(final Bundle client, final ServiceRegistration<Object> registration) {
                        final ClassLoader cl = bundle.adapt(BundleWiring.class).getClassLoader();
                        final ClassLoader oldTCCL = Thread.currentThread().getContextClassLoader();
                        Thread.currentThread().setContextClassLoader(cl);
                        try {
                            final Class<?> readerClass = cl.loadClass("org.apache.sling.feature.io.json.FeatureJSONReader");
                            final Method readMethod = readerClass.getDeclaredMethod("read", java.io.Reader.class, String.class);
                            try( final StringReader reader = new StringReader(featureSupplier.get())) {
                                return readMethod.invoke(null, reader, null);
                            }
                        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                            // ignore
                        } finally {
                            Thread.currentThread().setContextClassLoader(oldTCCL);
                        }
                        return null;
                    }

                    @Override
                    public void ungetService(final Bundle client, final ServiceRegistration<Object> registration,
                            final Object service) {
                        // nothing to do
                    }
                    
                }, properties);
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
                final URL f = this.installables.get(i);
                final Dictionary<String, Object> dict = new Hashtable<>();
                dict.put("resource.uri.hint", f.toURI().toString());
                final Object rsrc = constructor.newInstance(f.getPath(),
                        f.openStream(),
                        dict,
                        f.getPath(),
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
}
