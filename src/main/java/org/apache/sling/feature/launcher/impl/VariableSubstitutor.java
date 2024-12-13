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

import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;
import org.apache.sling.feature.launcher.spi.LauncherRunContext;

/** StringSubstitutor that uses a LauncherRunContext */
public class VariableSubstitutor extends StringSubstitutor {
    public VariableSubstitutor(LauncherRunContext context) {
        super(new StringLookup() {

            @Override
            public String lookup(final String key) {
                // Normally if a variable cannot be found, StrSubstitutor will
                // leave the raw variable in place. We need to replace it with
                // nothing in that case.
                final String value = context.getFrameworkProperties().get(key);
                return value == null ? "" : value;
            }
        });
        setEnableSubstitutionInVariables(true);
    }
}
