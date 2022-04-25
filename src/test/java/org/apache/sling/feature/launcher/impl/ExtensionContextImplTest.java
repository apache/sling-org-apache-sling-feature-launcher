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
package org.apache.sling.feature.launcher.impl;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.launcher.spi.LauncherPrepareContext;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ExtensionContextImplTest {
    @Test
    public void testGetFeature() throws Exception {
        URL testFeatureFile = getClass().getResource("/test-feature.json");

        ArtifactId aid2 = ArtifactId.fromMvnId("g:a:2");
        LauncherPrepareContext lpc = Mockito.mock(LauncherPrepareContext.class);
        Mockito.when(lpc.getArtifactFile(aid2)).thenReturn(testFeatureFile);

        ArtifactId aid1 = ArtifactId.fromMvnId("g:a:1");
        Feature f1 = new Feature(aid1);
        Map<ArtifactId, Feature> loaded = new HashMap<>();
        loaded.put(aid1, f1);

        ExtensionContextImpl c = new ExtensionContextImpl(lpc, null, loaded);
        assertEquals(f1, c.getFeature(aid1));
        assertNotNull(c.getFeature(aid2));
        assertNull(c.getFeature(ArtifactId.fromMvnId("g:a:3")));
    }
}
