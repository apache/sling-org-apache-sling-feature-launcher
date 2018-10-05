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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.BundleTracker;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class RegisterServiceReflectively {
    private final BundleContext bundleContext;
    private final String serviceClassName;
    private final String servicePackage;
    private final Dictionary<String, Object> serviceProperties;
    private final InvocationHandler invocationHandler;
    private final BundleTracker<Bundle> bundleTracker;
    private final Map<Bundle, ServiceRegistration<?>> registrations = new ConcurrentHashMap<>();

    RegisterServiceReflectively(BundleContext bc, String className, Dictionary<String, Object> props,
            InvocationHandler handler) {
        bundleContext = bc;
        serviceClassName = className;
        servicePackage = serviceClassName.substring(0, serviceClassName.lastIndexOf('.'));
        serviceProperties = props;
        invocationHandler = handler;

        bundleTracker = new BundleTracker<Bundle>(bc,
                Bundle.ACTIVE | Bundle.STOPPING | Bundle.UNINSTALLED, null) {
            @Override
            public Bundle addingBundle(Bundle bundle, BundleEvent event) {
                BundleWiring bw = bundle.adapt(BundleWiring.class);
                for (BundleCapability cap : bw.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE)) {
                    if (servicePackage.equals(cap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE))) {
                        try {
                            registrations.put(bundle, registerService(bw));
                        } catch (ClassNotFoundException e) {
                            // Ignore
                        }
                    }
                }

                return bundle;
            }

            @Override
            public void modifiedBundle(Bundle bundle, BundleEvent event, Bundle object) {
                removedBundle(bundle, event, object);
                addingBundle(bundle, event);
            }

            @Override
            public void removedBundle(Bundle bundle, BundleEvent event, Bundle object) {
                ServiceRegistration<?> reg = registrations.remove(bundle);
                if (reg != null)
                    reg.unregister();
            }
        };
    }

    protected ServiceRegistration<?> registerService(BundleWiring bw) throws ClassNotFoundException {
        Class<?> serviceClass = bw.getClassLoader().loadClass(serviceClassName);

        Object proxy = Proxy.newProxyInstance(bw.getClassLoader(), new Class [] {serviceClass}, invocationHandler);
        return bundleContext.registerService(serviceClassName, proxy, serviceProperties);
    }

    void open() {
        bundleTracker.open();
    }

    void close() {
        bundleTracker.close();
    }
}
