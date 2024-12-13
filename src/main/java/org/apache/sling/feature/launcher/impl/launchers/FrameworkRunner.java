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
package org.apache.sling.feature.launcher.impl.launchers;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

/**
 * Launcher directly using the OSGi launcher API.
 */
public class FrameworkRunner extends AbstractRunner {

    private static final String START_TIMEOUT = "sling.framework.start.timeout";

    private static final String SHUTDOWN_GRACE_TIME = "sling.framework.shutdown.graceTime";

    private volatile int type = -1;

    private final Map<String, String> frameworkProperties;

    private final Map<Integer, List<URL>> bundlesMap;

    public FrameworkRunner(
            final Map<String, String> frameworkProperties,
            final Map<Integer, List<URL>> bundlesMap,
            final List<Object[]> configurations,
            final List<URL> installables)
            throws Exception {
        super(configurations, installables);
        this.frameworkProperties = frameworkProperties;
        this.bundlesMap = bundlesMap;
    }

    /**
     * Get the framework factory
     * @return Return the first factory found via service loaders
     * @throws Exception If no factory can be found
     */
    protected FrameworkFactory getFrameworkFactory() throws Exception {
        final ServiceLoader<FrameworkFactory> loader = ServiceLoader.load(FrameworkFactory.class);
        FrameworkFactory factory = null;
        for (FrameworkFactory f : loader) {
            factory = f;
            break;
        }
        if (factory == null) {
            throw new Exception("Unable to locate framework factory.");
        }
        return factory;
    }

    public Integer call() throws Exception {
        // get the framework factory
        final FrameworkFactory factory = this.getFrameworkFactory();

        logger.info("Using framework factory {}", factory.getClass());
        // create the framework
        final Framework framework = factory.newFramework(frameworkProperties);
        // initialize the framework
        framework.init();

        long graceTime = Long.parseLong(frameworkProperties.getOrDefault(SHUTDOWN_GRACE_TIME, "60"));
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    framework.stop();
                    FrameworkEvent waitForStop = framework.waitForStop(graceTime * 1000);
                    if (waitForStop.getType() != FrameworkEvent.STOPPED) {
                        logger.warn("Framework stopped with: " + waitForStop.getType(), waitForStop.getThrowable());
                    } else {
                        logger.info("Framework stopped");
                    }
                } catch (BundleException | InterruptedException e) {
                    logger.warn("Exception stopping the framework in shutdown hook", e);
                }
            }
        });

        this.setupFramework(framework, bundlesMap);

        long time = System.currentTimeMillis();
        long startTimeout = Long.parseLong(frameworkProperties.getOrDefault(START_TIMEOUT, String.valueOf(10 * 60)));

        // finally start
        if (!this.startFramework(framework, startTimeout, TimeUnit.SECONDS)) {
            throw new TimeoutException("Waited for more than " + startTimeout + " seconds to startup framework.");
        }
        this.finishStartup(framework);
        logger.info("Framework started");

        logger.debug("Startup took: " + (System.currentTimeMillis() - time));

        while ((type = framework.waitForStop(Long.MAX_VALUE).getType()) == FrameworkEvent.STOPPED_UPDATE) {
            logger.info("Framework restart due to update");
            time = System.currentTimeMillis();
            if (!this.startFramework(framework, startTimeout, TimeUnit.SECONDS)) {
                throw new TimeoutException("Waited for more than " + startTimeout + " seconds to startup framework.");
            }
            logger.debug("Restart took: " + (System.currentTimeMillis() - time));
        }
        return type;
    }
}
