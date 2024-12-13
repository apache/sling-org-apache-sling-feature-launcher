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
package org.apache.sling.feature.launcher.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.feature.launcher.spi.LauncherRunContext;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Run this as part of integration tests to help make sure we
 *  embed the right Apache commons classes.
 */
public class VariableSubstitutorIT {
    private VariableSubstitutor vs;

    @Before
    public void setup() {
        final LauncherRunContext context = mock(LauncherRunContext.class);

        final Map<String, String> props = new HashMap<>();
        props.put("1", "one exactly");
        props.put("two", "here's ${1} and two");

        when(context.getFrameworkProperties()).thenReturn(props);
        vs = new VariableSubstitutor(context);
    }

    @Test
    public void simpleReplacement() {
        assertEquals("It is one exactly and two", vs.replace("It is ${1} and two"));
    }

    @Test
    public void recursiveReplacement() {
        assertEquals("Now here's one exactly and two for testing", vs.replace("Now ${two} for testing"));
    }

    @Test
    public void missingKey() {
        assertEquals("one exactly #", vs.replace("${1} ${notFoundOder}#"));
    }
}
