package org.apache.sling.feature.launcher.handler;

import org.apache.sling.feature.launcher.spi.extensions.ExtensionHandler;

public class ExtensionHandlerDefault
    extends ExtensionHandlerBase
    implements ExtensionHandler
{
    @Override
    public int getPriority() {
        return FALLBACK_PRIORITY;
    }
}
