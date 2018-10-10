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

import org.apache.sling.feature.launcher.impl.Main;
import org.apache.sling.feature.launcher.service.Bundles;
import org.apache.sling.feature.launcher.service.Features;
import org.apache.sling.feature.launcher.service.impl.BundlesImpl;
import org.apache.sling.feature.launcher.service.impl.FeaturesImpl;
import org.apache.sling.launchpad.api.LaunchpadContentProvider;
import org.apache.sling.launchpad.api.StartupHandler;
import org.apache.sling.launchpad.api.StartupMode;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

/**
 * Common functionality for the framework start.
 */
public abstract class AbstractRunner implements Callable<Integer> {

    private volatile ServiceTracker<Object, Object> configAdminTracker;

    private volatile ServiceTracker<Object, Object> installerTracker;

    private final List<Object[]> configurations;

    private final List<File> installables;

    private final int targetStartlevel;

    private final AtomicInteger waitRequested = new AtomicInteger(0);

    private volatile boolean install;

    public AbstractRunner(final Map<String, String> frameworkProperties, final List<Object[]> configurations, final List<File> installables) {
        this.configurations = new ArrayList<>(configurations);
        this.installables = installables;
        String target = frameworkProperties.get(Constants.FRAMEWORK_BEGINNING_STARTLEVEL);
        if (target != null) {
            targetStartlevel = Integer.parseInt(target);
        }
        else {
            targetStartlevel = 1;
        }
        if (!this.installables.isEmpty()) {
            if ("true".equalsIgnoreCase(frameworkProperties.get("sling.framework.install.incremental")))
            {
                frameworkProperties.put(Constants.FRAMEWORK_BEGINNING_STARTLEVEL, "1");
            }
        }
    }

    protected void setupFramework(final Framework framework, final Map<Integer, Map<String, File>> bundlesMap,
            String effectiveFeature)
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

        Map<Map.Entry<String, String>, String> bundleMap = null;
        try {
            bundleMap = this.install(framework, bundlesMap);
        } catch ( final IOException ioe) {
            throw new BundleException("Unable to install bundles.", ioe);
        }

        // TODO: double check bundles and take installables into account
        install = !this.configurations.isEmpty() || !this.installables.isEmpty() || !bundlesMap.isEmpty();
        try
        {
            // TODO: double check bundles and take installables into account
            final StartupMode mode = !install ? StartupMode.RESTART : StartupMode.INSTALL;

            framework.getBundleContext().registerService(StartupHandler.class, new StartupHandler()
            {
                @Override
                public StartupMode getMode()
                {
                    return mode;
                }

                @Override
                public boolean isFinished() {
                    return framework.getState() == Framework.ACTIVE && targetStartlevel > framework.adapt(FrameworkStartLevel.class).getStartLevel();
                }

                @Override
                public void waitWithStartup(boolean b) {
                    if (b) {
                        waitRequested.incrementAndGet();
                    }
                    else {
                        waitRequested.decrementAndGet();
                    }
                }
            }, null);

            framework.getBundleContext().registerService(LaunchpadContentProvider.class, new LaunchpadContentProvider()
            {
                @Override
                public Iterator<String> getChildren(String path) {
                    List<String> children;

                    // Guard against extra trailing slashes
                    if(path.endsWith("/") && path.length() > 1) {
                        path = path.substring(0, path.length()-1);
                    }

                    URL url = this.getClass().getResource(path);
                    if (url != null) {
                        Pattern pathPattern = Pattern.compile("^" + path + "/[^/]+/?$");

                        children = new ArrayList<String>();
                        try {
                            URLConnection conn = url.openConnection();
                            if (conn instanceof JarURLConnection) {
                                JarFile jar = ((JarURLConnection) conn).getJarFile();
                                Enumeration<JarEntry> entries = jar.entries();
                                while (entries.hasMoreElements()) {
                                    String entry = entries.nextElement().getName();
                                    if (pathPattern.matcher(entry).matches()) {
                                        children.add(entry);
                                    }
                                }
                            }
                        } catch (IOException ioe) {
                            // ignore for now
                        }
                    } else {
                        children = Collections.emptyList();
                    }

                    return children.iterator();
                }

                @Override
                public URL getResource(String path) {
                    // ensure path
                    if (path == null || path.length() == 0) {
                        return null;
                    }

                    // remove leading slash
                    if (path.charAt(0) == '/') {
                        path = path.substring(1);
                    }

                    return this.getResource(path);
                }

                @Override
                public InputStream getResourceAsStream(String path) {
                    URL res = this.getResource(path);
                    if (res != null) {
                        try {
                            return res.openStream();
                        } catch (IOException ioe) {
                            // ignore this one
                        }
                    }

                    // no resource
                    return null;

                }
            }, null);
        } catch (NoClassDefFoundError ex) {
            // Ignore, we don't have the launchpad.api
        }

        framework.getBundleContext().registerService(Bundles.class, new BundlesImpl(bundleMap), null);
        framework.getBundleContext().registerService(Features.class, new FeaturesImpl(effectiveFeature), null);
    }

    protected boolean startFramework(final Framework framework, long timeout, TimeUnit unit) throws BundleException, InterruptedException
    {
        CountDownLatch latch = new CountDownLatch(1);

        final Executor executor = Executors.newSingleThreadExecutor();

        FrameworkListener listener = new FrameworkListener()
        {
            @Override
            public void frameworkEvent(FrameworkEvent frameworkEvent)
            {
                if (frameworkEvent.getType() == FrameworkEvent.STARTED || frameworkEvent.getType() == FrameworkEvent.STARTLEVEL_CHANGED) {
                    if (framework.getState() == Framework.ACTIVE && targetStartlevel > framework.adapt(FrameworkStartLevel.class).getStartLevel()) {
                        if (install) {
                            executor.execute(() ->
                            {
                                if (waitRequested.get() == 0) {
                                    try {
                                        Thread.sleep(500);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                while (waitRequested.get() > 0) {
                                    try {
                                        Thread.sleep(500);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                try {
                                    framework.adapt(FrameworkStartLevel.class).setStartLevel(framework.adapt(FrameworkStartLevel.class).getStartLevel() + 1);
                                } catch (Exception ex) {
                                    latch.countDown();
                                }
                            });
                        }
                        else {
                            try {
                                framework.adapt(FrameworkStartLevel.class).setStartLevel(targetStartlevel);
                            } catch (Exception ex) {
                                latch.countDown();
                            }
                        }
                    }
                    else {
                        latch.countDown();
                    }
                }
            }
        };

        framework.getBundleContext().addFrameworkListener(listener);

        framework.start();

        try {
            return latch.await(timeout, unit);
        } finally {
            ((ExecutorService) executor).shutdownNow();
            framework.getBundleContext().removeFrameworkListener(listener);
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
            Main.LOG().error("Unable to create configurations", e);
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
    private Map<Map.Entry<String, String>, String> install(final Framework framework, final Map<Integer, Map<String, File>> bundleMap)
    throws IOException, BundleException {
        final Map<Map.Entry<String, String>, String> mapping = new HashMap<>();
        final BundleContext bc = framework.getBundleContext();
        final int defaultStartLevel = getProperty(bc, "felix.startlevel.bundle", 1);
        for(final Integer startLevel : sortStartLevels(bundleMap.keySet(), defaultStartLevel)) {
            Main.LOG().debug("Installing bundles with start level {}", startLevel);

            for(final Map.Entry<String, File> entry : bundleMap.get(startLevel).entrySet()) {
                File file = entry.getValue();
                Main.LOG().debug("- {}", file.getName());

                // use reference protocol. This avoids copying the binary to the cache directory
                // of the framework
                final Bundle bundle = bc.installBundle("reference:" + file.toURI().toURL(), null);

                // Record the mapping of the feature model bundle artifact ID to the Bundle Symbolic Name and Version
                mapping.put(new AbstractMap.SimpleEntry<String, String>(
                        bundle.getSymbolicName(), bundle.getVersion().toString()), entry.getKey());

                // fragment?
                if ( !isSystemBundleFragment(bundle) && getFragmentHostHeader(bundle) == null ) {
                    if ( startLevel > 0 ) {
                        bundle.adapt(BundleStartLevel.class).setStartLevel(startLevel);
                    }
                    bundle.start();
                }
            }
        }
        return mapping;
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
            Main.LOG().error("Unable to contact installer and install additional artifacts", e);
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
