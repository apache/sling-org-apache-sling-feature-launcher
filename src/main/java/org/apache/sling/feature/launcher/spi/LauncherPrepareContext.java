/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.feature.launcher.spi;

import java.io.IOException;
import java.net.URL;

import org.apache.sling.feature.ArtifactId;
import org.slf4j.Logger;

/**
 * This is the context for the launcher
 */
public interface LauncherPrepareContext {

    /**
     * Get the central logger
     *
     * @return The logger
     */
    Logger getLogger();

    /**
     * Add an application jar URL to the classpath.
     *
     * @param url the URL to add
     */
    void addAppJar(URL url);

    /**
     * Get the file URL for an artifact.
     *
     * @param artifact the artifact ID
     * @return the URL of the artifact file
     * @throws IOException if the artifact file cannot be resolved or accessed
     */
    URL getArtifactFile(ArtifactId artifact) throws IOException;
}
