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
package org.apache.sling.feature.launcher.impl.extensions.handlers;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.launcher.spi.LauncherPrepareContext;
import org.apache.sling.feature.launcher.spi.extensions.ExtensionHandler;
import org.apache.sling.feature.launcher.spi.extensions.ExtensionInstallationContext;

public class RepoInitHandler implements ExtensionHandler
{
    private static final AtomicInteger index = new AtomicInteger(1);

    @Override
    public boolean handle(Extension extension, LauncherPrepareContext prepareContext, ExtensionInstallationContext installationContext) throws Exception
    {
        if (extension.getName().equals(Extension.EXTENSION_NAME_REPOINIT)) {
            if ( extension.getType() != ExtensionType.TEXT ) {
                throw new Exception(Extension.EXTENSION_NAME_REPOINIT + " extension must be of type text");
            }
            final Configuration cfg = new Configuration("org.apache.sling.jcr.repoinit.RepositoryInitializer~repoinit"
                    + String.valueOf(index.getAndIncrement()));
            cfg.getProperties().put("scripts", extension.getText());
            installationContext.addConfiguration(Configuration.getName(cfg.getPid()),
                    Configuration.getFactoryPid(cfg.getPid()), cfg.getConfigurationProperties());
            return true;
        }
        return false;
    }
}
