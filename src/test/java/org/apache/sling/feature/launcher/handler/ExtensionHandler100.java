package org.apache.sling.feature.launcher.handler;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.launcher.spi.extensions.ExtensionContext;
import org.apache.sling.feature.launcher.spi.extensions.ExtensionHandler;

public class ExtensionHandler100
    extends ExtensionHandlerBase
    implements ExtensionHandler
{
    @Override
    public int getPriority() {
        return 100;
    }
}
