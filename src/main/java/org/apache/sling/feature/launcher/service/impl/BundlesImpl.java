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
package org.apache.sling.feature.launcher.service.impl;

import org.apache.sling.feature.launcher.service.Bundles;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Map.Entry;

public class BundlesImpl implements Bundles {
    private final Map<Map.Entry<String, String>, String> bundleMap;

    public BundlesImpl(Map<Entry<String, String>, String> bm) {
        bundleMap = bm;
    }

    @Override
    public String getBundleArtifact(String bsn, String ver) {
        return bundleMap.get(new AbstractMap.SimpleEntry<String, String>(bsn, ver));
    }
}
