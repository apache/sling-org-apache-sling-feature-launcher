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

import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.wiring.BundleRevision;

public class AbstractRunnerTest {
    @Test
    public void testBundlesFragmentsStarted() {
        BundleRevision br1 = Mockito.mock(BundleRevision.class);
        Mockito.when(br1.getTypes()).thenReturn(BundleRevision.TYPE_FRAGMENT);
        Bundle b1 = Mockito.mock(Bundle.class);
        Mockito.when(b1.adapt(BundleRevision.class)).thenReturn(br1);
        Mockito.when(b1.getState()).thenReturn(Bundle.RESOLVED);

        Bundle b2 = Mockito.mock(Bundle.class);
        Mockito.when(b2.adapt(BundleRevision.class)).thenReturn(Mockito.mock(BundleRevision.class));
        Mockito.when(b2.getState()).thenReturn(Bundle.ACTIVE);

        Bundle b3 = Mockito.mock(Bundle.class);
        Mockito.when(b3.adapt(BundleRevision.class)).thenReturn(Mockito.mock(BundleRevision.class));
        Mockito.when(b3.getState()).thenReturn(Bundle.ACTIVE);

        BundleContext bc = Mockito.mock(BundleContext.class);
        Mockito.when(bc.getProperty("sling.feature.launcher.failonerror")).thenReturn("true");
        Mockito.when(bc.getBundles()).thenReturn(new Bundle[] {b1, b2, b3});

        Framework fw = Mockito.mock(Framework.class);
        Mockito.when(fw.getBundleContext()).thenReturn(bc);

        AbstractRunner.checkResultingState(fw);
    }

    @Test(expected=IllegalStateException.class)
    public void testBundleNotStarted() {
        BundleRevision br1 = Mockito.mock(BundleRevision.class);
        Mockito.when(br1.getTypes()).thenReturn(BundleRevision.TYPE_FRAGMENT);
        Bundle b1 = Mockito.mock(Bundle.class);
        Mockito.when(b1.adapt(BundleRevision.class)).thenReturn(br1);
        Mockito.when(b1.getState()).thenReturn(Bundle.RESOLVED);

        Bundle b2 = Mockito.mock(Bundle.class);
        Mockito.when(b2.adapt(BundleRevision.class)).thenReturn(Mockito.mock(BundleRevision.class));
        Mockito.when(b2.getState()).thenReturn(Bundle.ACTIVE);

        Bundle b3 = Mockito.mock(Bundle.class);
        Mockito.when(b3.adapt(BundleRevision.class)).thenReturn(Mockito.mock(BundleRevision.class));
        Mockito.when(b3.getState()).thenReturn(Bundle.INSTALLED);

        BundleContext bc = Mockito.mock(BundleContext.class);
        Mockito.when(bc.getProperty("sling.feature.launcher.failonerror")).thenReturn("true");
        Mockito.when(bc.getBundles()).thenReturn(new Bundle[] {b1, b2, b3});

        Framework fw = Mockito.mock(Framework.class);
        Mockito.when(fw.getBundleContext()).thenReturn(bc);

        // Should throw IllegalStateException
        AbstractRunner.checkResultingState(fw);
    }

    @Test
    public void testBundleNotStartedButNotEnabled() {
        Bundle b1 = Mockito.mock(Bundle.class);
        Mockito.when(b1.adapt(BundleRevision.class)).thenReturn(Mockito.mock(BundleRevision.class));
        Mockito.when(b1.getState()).thenReturn(Bundle.INSTALLED);

        BundleContext bc = Mockito.mock(BundleContext.class);
        Mockito.when(bc.getBundles()).thenReturn(new Bundle[] {b1});

        Framework fw = Mockito.mock(Framework.class);
        Mockito.when(fw.getBundleContext()).thenReturn(bc);

        // Should throw IllegalStateException
        AbstractRunner.checkResultingState(fw);
    }
}
