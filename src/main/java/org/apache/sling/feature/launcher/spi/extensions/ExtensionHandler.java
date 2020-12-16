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
package org.apache.sling.feature.launcher.spi.extensions;

import org.apache.sling.feature.Extension;

/**
 * A extension handler can be used to add additional functionality to the launcher
 * based on extension in the feature model. For example, the Apache Sling specific
 * extension for repoinit is unknown to the launcher. An extension handler can be
 * used to handle that extension and provide the information to the runtime.
 * Before launching, the extension handlers are called until a handler returns {@code true}
 * for an extension. Therefore only one handler can be invoked for a given extension.
 * An extension handler is needed for every required extension in the feature model.
 */
public interface ExtensionHandler {

   /**
     * Try to handle the extension. As soon as a handler returns {@code true},
     * no other handler is invoked for this extension.
     *
     * @param context  Context for the handler
     * @param extension The feature model extension
     * @return {@code true} if the handler handled the extension.
     * @throws Exception If an error occurs during processing of the extension.
     */    
    public boolean handle(ExtensionContext context, Extension extension) throws Exception;
}
