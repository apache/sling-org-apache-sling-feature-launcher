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

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.launcher.impl.Main;
import org.apache.sling.feature.launcher.spi.Launcher;
import org.apache.sling.feature.launcher.spi.LauncherPrepareContext;
import org.apache.sling.feature.launcher.spi.LauncherRunContext;
import org.osgi.framework.Constants;

/**
 * Launcher directly using the OSGi launcher API.
 */
public class FrameworkLauncher implements Launcher {


    @Override
    public void prepare(final LauncherPrepareContext context,
            final ArtifactId frameworkId,
            final Feature app) throws Exception {
        context.addAppJar(context.getArtifactFile(frameworkId));
        ArtifactId api = ArtifactId.fromMvnId("org.apache.sling:org.apache.sling.launchpad.api:1.2.0");
        Artifact artifact = app.getBundles().getSame(api);
        if (artifact != null)
        {
            api = artifact.getId();
            context.addAppJar(context.getArtifactFile(api));
            app.getBundles().removeExact(api);
            String extra = app.getFrameworkProperties().get(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA);
            if (extra != null && !extra.trim().isEmpty()) {
                extra = extra + ",";
            }
            else {
                extra = "";
            }
            extra = extra + "org.apache.sling.launchpad.api;version=\"" + api.getOSGiVersion() + "\"";
            app.getFrameworkProperties().put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, extra);
        }
    }

    /**
     * Run the launcher
     * @throws Exception If anything goes wrong
     */
    @Override
    public int run(final LauncherRunContext context, final ClassLoader cl) throws Exception {
        StrSubstitutor ss = new StrSubstitutor(new StrLookup() {
            @Override
            public String lookup(String key) {
                // Normally if a variable cannot be found, StrSubstitutor will
                // leave the raw variable in place. We need to replace it with
                // nothing in that case.

                String val = context.getFrameworkProperties().get(key);
                return val != null ? val : "";
            }
        });
        ss.setEnableSubstitutionInVariables(true);

        Map<String, String> properties = new HashMap<>();
        context.getFrameworkProperties().forEach((key, value) -> {
            properties.put(key, ss.replace(value).replace("{dollar}", "$"));
        });
        if ( Main.LOG().isDebugEnabled() ) {
            Main.LOG().debug("Bundles:");
            for(final Integer key : context.getBundleMap().keySet()) {
                Main.LOG().debug("-- Start Level {}", key);
                for(final File f : context.getBundleMap().get(key).values()) {
                    Main.LOG().debug("  - {}", f.getName());
                }
            }
            Main.LOG().debug("Settings: ");
            for(final Map.Entry<String, String> entry : properties.entrySet()) {
                Main.LOG().debug("- {}={}", entry.getKey(), entry.getValue());
            }
            Main.LOG().debug("Configurations: ");
            for(final Object[] entry : context.getConfigurations()) {
                if ( entry[1] != null ) {
                    Main.LOG().debug("- Factory {} - {}", entry[1], entry[0]);
                } else {
                    Main.LOG().debug("- {}", entry[0]);
                }
            }
            Main.LOG().debug("");
        }

        final Class<?> runnerClass = cl.loadClass(FrameworkRunner.class.getName());
        final Constructor<?> constructor = runnerClass.getDeclaredConstructor(Map.class, Map.class, List.class, List.class);
        constructor.setAccessible(true);
        Callable<Integer> restart = (Callable<Integer>) constructor.newInstance(properties,
                context.getBundleMap(),
                context.getConfigurations(),
                context.getInstallableArtifacts(),
                context.getEffectiveFeature());

        return restart.call();
        // nothing else to do, constructor starts everything
    }
}
