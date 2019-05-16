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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.sling.feature.io.file.ArtifactHandler;
import org.apache.sling.feature.launcher.spi.LauncherRunContext;
import org.slf4j.Logger;

/**
 * This class holds the configuration of the launcher.
 */
public class Installation implements LauncherRunContext {

    /** The map with the framework properties. */
    private final Map<String, String> fwkProperties = new HashMap<>();

    /** Bundle map */
    private final Map<Integer, List<ArtifactHandler>> bundleMap = new HashMap<>();

    /** Artifacts to be installed */
    private final List<ArtifactHandler> installables = new ArrayList<>();

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
     * @param file The bundle file
     */
    public void addBundle(final Integer startLevel, final File file) {
        List<ArtifactHandler> files = bundleMap.get(startLevel);
        if ( files == null ) {
            files = new ArrayList<>();
            bundleMap.put(startLevel, files);
        }
        files.add(convert(file));
    }

    /**
     * Add a bundle with the given start level
     * @param startLevel The start level
     * @param file The bundle file
     */
    public void addBundle(final Integer startLevel, final ArtifactHandler file) {
        List<ArtifactHandler> files = bundleMap.get(startLevel);
        if ( files == null ) {
            files = new ArrayList<>();
            bundleMap.put(startLevel, files);
        }
        files.add(file);
    }

    /**
     * Add an artifact to be installed by the installer
     * @param file The file
     */
    public void addInstallableArtifact(final File file) {
        this.installables.add(convert(file));
    }

    /**
     * Add a configuration
     * @param pid The pid
     * @param factoryPid The factory pid
     * @param properties The propertis
     */
    public void addConfiguration(final String pid, final String factoryPid, final Dictionary<String, Object> properties) {
        this.configurations.add(new Object[] {pid, factoryPid, properties});
    }

    /**
     * @see org.apache.sling.feature.launcher.spi.LauncherRunContext#getFrameworkProperties()
     */
    @Override
    public Map<String, String> getFrameworkProperties() {
        return this.fwkProperties;
    }

    public void addFrameworkProperty(String key, String value)
    {
        this.fwkProperties.put(key, value);
    }

    /**
     * @see org.apache.sling.feature.launcher.spi.LauncherRunContext#getBundleMap()
     */
    @Override
    public Map<Integer, List<File>> getBundleMap() {
        return convert(this.bundleMap);
    }

    @Override
    public Map<Integer, List<ArtifactHandler>> getBundleArtifactMap()
    {
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
    public List<File> getInstallableArtifacts() {
        return convertHandlers(this.installables);
    }

    @Override
    public List<ArtifactHandler> getInstallableArtifactHandlers()
    {
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

    private ArtifactHandler convert(File file) {
        if (file == null) {
            return null;
        }
        try
        {
            return new ArtifactHandler(file.toURI().toURL().toExternalForm(), file);
        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException(e);
        }
    }

    private List<File> convertHandlers(List<ArtifactHandler> list) {
        if (list == null) {
            return null;
        }
        return new AbstractList<File>()
        {
            @Override
            public File get(int index)
            {
                return list.get(index).getFile();
            }

            @Override
            public int size()
            {
                return list.size();
            }

            @Override
            public File remove(int index)
            {
                ArtifactHandler handler = list.remove(index);
                return handler != null ? handler.getFile() : null;
            }

            @Override
            public File set(int index, File element)
            {
                ArtifactHandler handler = list.set(index, convert(element));
                return handler != null ? handler.getFile() : null;
            }

            @Override
            public void add(int index, File element)
            {
                list.add(index, convert(element));
            }
        };
    }

    private List<ArtifactHandler> convertFiles(List<File> list) {
        if (list == null) {
            return null;
        }
        return new AbstractList<ArtifactHandler>()
        {
            @Override
            public ArtifactHandler get(int index)
            {
                return convert(list.get(index));
            }

            @Override
            public int size()
            {
                return list.size();
            }

            @Override
            public ArtifactHandler remove(int index)
            {
                return convert(list.remove(index));
            }

            @Override
            public ArtifactHandler set(int index, ArtifactHandler element)
            {
                return convert(list.set(index, element != null ? element.getFile() : null));
            }

            @Override
            public void add(int index, ArtifactHandler element)
            {
                list.add(index, element != null ? element.getFile() : null);
            }
        };
    }

    private Map<Integer, List<File>> convert(Map<Integer, List<ArtifactHandler>> map) {
        return new AbstractMap<Integer, List<File>>()
        {
            @Override
            public Set<Entry<Integer, List<File>>> entrySet()
            {
                Set<Map.Entry<Integer, List<ArtifactHandler>>> set = map.entrySet();
                return new AbstractSet<Entry<Integer, List<File>>>()
                {
                    @Override
                    public Iterator<Entry<Integer, List<File>>> iterator()
                    {
                        Iterator<Map.Entry<Integer, List<ArtifactHandler>>> iter = set.iterator();
                        return new Iterator<Entry<Integer, List<File>>>()
                        {
                            @Override
                            public boolean hasNext()
                            {
                                return iter.hasNext();
                            }

                            @Override
                            public Entry<Integer, List<File>> next()
                            {
                                Entry<Integer, List<ArtifactHandler>> entry = iter.next();
                                return new AbstractMap.SimpleEntry<Integer, List<File>>(entry.getKey(), convertHandlers(entry.getValue())) {
                                    @Override
                                    public List<File> setValue(List<File> value)
                                    {
                                        entry.setValue(convertFiles(value));
                                        return super.setValue(value);
                                    }
                                };
                            }

                            @Override
                            public void remove()
                            {
                                iter.remove();
                            }
                        };
                    }

                    @Override
                    public int size()
                    {
                        return set.size();
                    }
                };
            }

            @Override
            public List<File> put(Integer key, List<File> value)
            {
                return convertHandlers(map.put(key, convertFiles(value)));
            }
        };
    }
}
