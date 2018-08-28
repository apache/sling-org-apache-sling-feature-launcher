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
package org.apache.sling.feature.launcher.spi.extensions;

import java.io.File;
import java.util.Dictionary;

import org.apache.sling.feature.launcher.spi.LauncherRunContext;

public interface ExtensionInstallationContext extends LauncherRunContext
{
    public void addBundle(final Integer startLevel, final File file);

    /**
     * Add an artifact to be installed by the installer
     * @param file The file
     */
    public void addInstallableArtifact(final File file);

    /**
     * Add a configuration
     * @param pid The pid
     * @param factoryPid The factory pid
     * @param properties The propertis
     */
    public void addConfiguration(final String pid, final String factoryPid, final Dictionary<String, Object> properties);

    public void addFrameworkProperty(final String key, final String value);
}
