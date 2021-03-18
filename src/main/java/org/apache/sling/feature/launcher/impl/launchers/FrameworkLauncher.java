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

import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.launcher.impl.VariableSubstitutor;
import org.apache.sling.feature.launcher.spi.Launcher;
import org.apache.sling.feature.launcher.spi.LauncherPrepareContext;
import org.apache.sling.feature.launcher.spi.LauncherRunContext;

import aQute.bnd.annotation.spi.ServiceProvider;

/**
 * Launcher directly using the OSGi launcher API.
 */
@ServiceProvider(value = Launcher.class)
public class FrameworkLauncher implements Launcher {


    @Override
    public void prepare(final LauncherPrepareContext context,
            final ArtifactId frameworkId,
            final Feature app) throws Exception {
        context.addAppJar(context.getArtifactFile(frameworkId));
    }

    /**
     * Run the launcher
     * @throws Exception If anything goes wrong
     */
    @Override
    public int run(final LauncherRunContext context, final ClassLoader cl) throws Exception {
        final VariableSubstitutor vs = new VariableSubstitutor(context);

        Map<String, String> properties = new HashMap<>();
        context.getFrameworkProperties().forEach((key, value) -> {
            properties.put(key, vs.replace(value).replace("{dollar}", "$"));
        });
        if (context.getLogger().isDebugEnabled()) {
            context.getLogger().debug("Bundles:");
            for(final Integer key : context.getBundleMap().keySet()) {
                context.getLogger().debug("-- Start Level {}", key);
                for(final URL f : context.getBundleMap().get(key)) {
                    context.getLogger().debug("  - {}", f);
                }
            }
            context.getLogger().debug("Settings: ");
            for(final Map.Entry<String, String> entry : properties.entrySet()) {
                context.getLogger().debug("- {}={}", entry.getKey(), entry.getValue());
            }
            context.getLogger().debug("Configurations: ");
            for(final Object[] entry : context.getConfigurations()) {
                if ( entry[1] != null ) {
                    context.getLogger().debug("- Factory {} - {}", entry[1], entry[0]);
                } else {
                    context.getLogger().debug("- {}", entry[0]);
                }
            }
            context.getLogger().debug("");
        }

        final Class<?> runnerClass = cl.loadClass(getFrameworkRunnerClass());
        final Constructor<?> constructor = runnerClass.getDeclaredConstructor(Map.class, Map.class, List.class,
                List.class);
        constructor.setAccessible(true);
        @SuppressWarnings("unchecked")
        Callable<Integer> restart = (Callable<Integer>) constructor.newInstance(properties, context.getBundleMap(),
                context.getConfigurations(), context.getInstallableArtifacts());

        return restart.call();
        // nothing else to do, constructor starts everything
    }

    protected String getFrameworkRunnerClass() {
        return FrameworkRunner.class.getName();
    }
}
