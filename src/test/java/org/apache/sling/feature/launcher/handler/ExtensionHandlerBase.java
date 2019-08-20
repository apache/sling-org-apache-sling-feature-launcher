package org.apache.sling.feature.launcher.handler;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.launcher.spi.extensions.ExtensionContext;
import org.apache.sling.feature.launcher.spi.extensions.ExtensionHandler;

public abstract class ExtensionHandlerBase
    implements ExtensionHandler
{
    private static int lastPriorityUsed = -100;

    public static int getLastPriorityUsed() {
        return lastPriorityUsed;
    }

    @Override
    public boolean handle(ExtensionContext context, Extension extension) throws Exception {
        boolean answer = lastPriorityUsed == -100;
        lastPriorityUsed = getPriority();
        return answer;
    }
}
