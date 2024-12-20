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
package org.apache.sling.feature.launcher.impl.extensions.handlers;

import java.io.IOException;

import aQute.bnd.annotation.spi.ServiceProvider;
import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.launcher.spi.extensions.ExtensionContext;
import org.apache.sling.feature.launcher.spi.extensions.ExtensionHandler;

@ServiceProvider(value = ExtensionHandler.class)
public class ContentPackageHandler implements ExtensionHandler {
    @Override
    public boolean handle(ExtensionContext context, Extension extension) throws IOException {
        if (extension.getType() == ExtensionType.ARTIFACTS
                && extension.getName().equals(Extension.EXTENSION_NAME_CONTENT_PACKAGES)) {
            for (final Artifact a : extension.getArtifacts()) {
                context.addInstallableArtifact(context.getArtifactFile(a.getId()));
            }
            return true;
        } else {
            return false;
        }
    }
}
