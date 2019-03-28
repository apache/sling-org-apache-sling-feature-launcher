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

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.launcher.spi.LauncherPrepareContext;

import java.io.IOException;

/**
 * This context object is provided to launcher extensions.
 */
public interface ExtensionContext extends LauncherPrepareContext, ExtensionInstallationContext {
    /**
     * Return the feature object for a given Artifact ID. It looks for the requested feature
     * in the list of features provided from the launcher commandline as well as in the configured
     * repositories.
     * @param artifact The artifact ID for the feature.
     * @return The Feature Model or null if the artifact cannot be found.
     * @throws IOException If the artifact can be found, but creating a Feature
     * Model out of it causes an exception.
     */
    Feature getFeature(ArtifactId artifact) throws IOException;
}
