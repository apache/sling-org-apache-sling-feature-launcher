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

import org.junit.Test;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class MainTest {
    @Test
    public void testSplitCommandlineArgs() {
        assertArrayEquals(new String[] {"hi", "ho"}, Main.split("hi=ho"));
        assertArrayEquals(new String[] {"hi.de.hi", "true"}, Main.split("hi.de.hi"));
    }

    @Test
    public void testSplitMapCommandlineArgs() {
        assertEquals(new AbstractMap.SimpleEntry<>("foo", Collections.singletonMap("bar", "tar")),
                Main.splitMap("foo:bar=tar"));

        assertEquals(new AbstractMap.SimpleEntry<>("hello", Collections.emptyMap()),
                Main.splitMap("hello"));

        Map<String,String> em = new HashMap<>();
        em.put("a.b.c", "d.e.f");
        em.put("h.i.j", "k.l.m");
        Map.Entry<String, Map<String, String>> e = new AbstractMap.SimpleEntry<>("ding.dong", em);
        assertEquals(e,
                Main.splitMap("ding.dong:a.b.c=d.e.f,h.i.j=k.l.m"));
    }
}
