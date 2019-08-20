package org.apache.sling.feature.launcher.impl;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Extensions;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.launcher.handler.ExtensionHandlerBase;
import org.apache.sling.feature.launcher.spi.LauncherPrepareContext;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class FeatureProcessorTest {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    public void testExtensionHandlerSorting() throws Exception {
        LauncherPrepareContext mockContext = mock(LauncherPrepareContext.class);
        LauncherConfig mockConfig = mock(LauncherConfig.class);
        Feature feature = spy(new Feature(new ArtifactId("g", "a","1.0.0", "test", "jar" )));
        Map<ArtifactId, Feature> loadedFeatures = new HashMap<>();
        Extension extension = new Extension(
            ExtensionType.TEXT,
            "ContentHandler",
            true
        );
        Extensions extensions = new Extensions();
        extensions.add(extension);
        when(feature.getExtensions()).thenReturn(extensions);

        FeatureProcessor.prepareLauncher(
            mockContext, mockConfig, feature, loadedFeatures
        );
        logger.info("Last Priority Used: '{}'", ExtensionHandlerBase.getLastPriorityUsed());
        assertEquals("Wrong Priority was used", 200, ExtensionHandlerBase.getLastPriorityUsed());
    }
}
