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
package org.apache.sling.feature.launcher.spi;

import java.net.URL;
import java.net.URLClassLoader;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;

public interface Launcher {

    void prepare(LauncherPrepareContext context, ArtifactId frameworkId, Feature app) throws Exception;

    int run(LauncherRunContext context, ClassLoader cl) throws Exception;

    default LauncherClassLoader createClassLoader() {
        return new LauncherClassLoader();
    }

    class LauncherClassLoader extends URLClassLoader {
        static {
            ClassLoader.registerAsParallelCapable();
        }
        public LauncherClassLoader() {
            super(new URL[0]);
        }

        @Override
        public final void addURL(URL url) {
            super.addURL(url);
        }

        @Override
        public final Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
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
    }
}
