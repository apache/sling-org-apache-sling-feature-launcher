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

import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.sling.feature.ArtifactId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the launcher main class. It parses command line parameters and
 * prepares the launcher.
 */
public class Main {

    public static final String OPT_OSGI_FRAMEWORK_ARTIFACT = "fa";

    public static final String OPT_FELIX_FRAMEWORK_VERSION = "fv";

    public static final String OPT_EXTENSION_CONFIGURATION = "ec";

    public static final String OPT_HOME_DIR = "p";

    public static final String OPT_CACHE_DIR = "c";

    public static final String OPT_VERBOSE = "v";

    public static final String OPT_VARIABLE_VALUES = "V";

    public static final String OPT_FRAMEWORK_PROPERTIES = "D";

    public static final String OPT_FEATURE_FILES = "f";

    public static final String OPT_REPOSITORY_URLS = "u";

    public static final String OPT_CONFIG_CLASH = "CC";

    public static final String OPT_ARTICACT_CLASH = "C";

    private static Logger LOGGER;

    private static Logger LOG() {

        if (LOGGER == null) {
            LOGGER = LoggerFactory.getLogger("launcher");
        }
        return LOGGER;
    }

    /** Split a string into key and value */
    static String[] splitKeyVal(final String keyVal) {

        final int pos = keyVal.indexOf('=');
        if (pos == -1) {
            return new String[] { keyVal, "true" };
        }
        return new String[] { keyVal.substring(0, pos), keyVal.substring(pos + 1) };
    }

    static Map.Entry<String, Map<String, String>> splitMap(final String val) {

        String[] split1 = val.split(":");

        if (split1.length < 2) {
            return new AbstractMap.SimpleEntry<>(split1[0], Collections.emptyMap());
        }

        Map<String, String> m = new HashMap<>();
        for (String kv : split1[1].split(",")) {
            String[] keyval = splitKeyVal(kv);
            m.put(keyval[0], keyval[1]);
        }

        return new AbstractMap.SimpleEntry<>(split1[0], m);
    }

    private static Optional<String> extractValueFromOption(CommandLine cl, String opt) {

        return extractValueFromOption(cl, opt, null);
    }

    private static Optional<String> extractValueFromOption(CommandLine cl, String opt,
            String defaultVaue) {

        return Optional.ofNullable(cl.getOptionValue(opt, defaultVaue));
    }

    private static Optional<List<String>> extractValuesFromOption(CommandLine cl, String opt) {

        String[] values = cl.getOptionValues(opt);
        if (Objects.isNull(values)) {
            return Optional.empty();
        }
        return Optional.of(Stream.of(values).collect(Collectors.toList()));
    }

    /**
     * Parse the command line parameters and update a configuration object.
     * 
     * @param args Command line parameters
     * @return Configuration object.
     */
    protected static void parseArgs(final LauncherConfig config, final String[] args) {

        final Option artifactClashOverride = Option.builder(OPT_ARTICACT_CLASH)
                .longOpt("artifact-clash")
                .desc("Set artifact clash override")
                .optionalArg(true)
                .numberOfArgs(Option.UNLIMITED_VALUES)
                .build();

        final Option configClashOverride = Option.builder(OPT_CONFIG_CLASH)
                .longOpt("config-clash")
                .desc("Set config clash override")
                .optionalArg(true)
                .numberOfArgs(Option.UNLIMITED_VALUES)
                .build();

        final Option repoOption = Option.builder(OPT_REPOSITORY_URLS)
                .longOpt("repository-urls")
                .desc("Set repository urls")
                .optionalArg(true)
                .valueSeparator(',')
                .numberOfArgs(Option.UNLIMITED_VALUES)
                .build();

        final Option featureOption = Option.builder(OPT_FEATURE_FILES)
                .longOpt("feature-files")
                .desc("Set feature files")
                .optionalArg(true)
                .valueSeparator(',')
                .numberOfArgs(Option.UNLIMITED_VALUES)
                .build();

        final Option fwkProperties = Option.builder(OPT_FRAMEWORK_PROPERTIES)
                .longOpt("framework-properties")
                .desc("Set framework properties")
                .optionalArg(true)
                .numberOfArgs(Option.UNLIMITED_VALUES)
                .build();

        final Option varValue = Option.builder(OPT_VARIABLE_VALUES)
                .longOpt("variable-values")
                .desc("Set variable values")
                .optionalArg(true)
                .numberOfArgs(Option.UNLIMITED_VALUES)
                .build();

        final Option debugOption = Option.builder(OPT_VERBOSE)
                .longOpt("verbose")
                .desc("Verbose")
                .optionalArg(true)
                .numberOfArgs(1)
                .build();

        final Option cacheOption = Option.builder(OPT_CACHE_DIR)
                .longOpt("cache_dir")
                .desc("Set cache dir")
                .optionalArg(true)
                .numberOfArgs(1)
                .build();

        final Option homeOption = Option.builder(OPT_HOME_DIR)
                .longOpt("home_dir")
                .desc("Set home dir")
                .optionalArg(true)
                .numberOfArgs(1)
                .build();

        final Option extensionConfiguration = Option.builder(OPT_EXTENSION_CONFIGURATION)
                .longOpt("extension_configuration")
                .desc("Provide extension configuration, format: extensionName:key1=val1,key2=val2")
                .optionalArg(true)
                .numberOfArgs(Option.UNLIMITED_VALUES)
                .build();

        final Option frameworkVersionOption = Option.builder(OPT_FELIX_FRAMEWORK_VERSION)
                .longOpt("felix-framework-version")
                .desc("Set Apache Felix framework version (default "
                        .concat(Bootstrap.FELIX_FRAMEWORK_VERSION) + ")")
                .optionalArg(true)
                .numberOfArgs(1)
                .build();

        final Option frameworkArtifactOption = Option.builder(OPT_OSGI_FRAMEWORK_ARTIFACT)
                .longOpt("osgi-framework-artifact")
                .desc("Set OSGi framework artifact (overrides Apache Felix framework version)")
                .optionalArg(true)
                .numberOfArgs(1)
                .build();

        final Options options = new Options().addOption(artifactClashOverride)
                .addOption(configClashOverride)
                .addOption(repoOption)
                .addOption(featureOption)
                .addOption(fwkProperties)
                .addOption(varValue)
                .addOption(debugOption)
                .addOption(cacheOption)
                .addOption(homeOption)
                .addOption(extensionConfiguration)
                .addOption(frameworkVersionOption)
                .addOption(frameworkArtifactOption);

        final CommandLineParser clp = new DefaultParser();
        try {
            final CommandLine cl = clp.parse(options, args);

            extractValuesFromOption(cl, OPT_REPOSITORY_URLS).ifPresent(
                    values -> config.setRepositoryUrls(values.stream().toArray(String[]::new)));

            extractValuesFromOption(cl, OPT_ARTICACT_CLASH).ifPresent(values -> values
                    .forEach(v -> config.getArtifactClashOverrides().add(ArtifactId.parse(v))));

            Properties cfgCProps = cl.getOptionProperties(OPT_CONFIG_CLASH);
            for (final String name : cfgCProps.stringPropertyNames()) {
                config.getConfigClashOverrides().put(name, cfgCProps.getProperty(name));
            }

            Properties fwProps = cl.getOptionProperties(OPT_FRAMEWORK_PROPERTIES);
            for (final String name : fwProps.stringPropertyNames()) {
                config.getInstallation()
                        .getFrameworkProperties()
                        .put(name, fwProps.getProperty(name));
            }

            Properties varProps = cl.getOptionProperties(OPT_VARIABLE_VALUES);
            for (final String name : varProps.stringPropertyNames()) {
                config.getVariables().put(name, varProps.getProperty(name));
            }

            extractValueFromOption(cl, OPT_VERBOSE, "debug").ifPresent(
                    value -> System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", value));

            extractValuesFromOption(cl, OPT_FEATURE_FILES).orElseGet(ArrayList::new)
                    .forEach(config::addFeatureFiles);

            extractValueFromOption(cl, OPT_CACHE_DIR).map(File::new)
                    .ifPresent(config::setCacheDirectory);

            extractValueFromOption(cl, OPT_HOME_DIR).map(File::new)
                    .ifPresent(config::setHomeDirectory);

            extractValuesFromOption(cl, OPT_EXTENSION_CONFIGURATION)
                    .ifPresent(values -> values.forEach(v -> {
                        Map.Entry<String, Map<String, String>> xc = splitMap(v);
                        Map<String, Map<String, String>> ec = config.getExtensionConfiguration();
                        Map<String, String> c = ec.get(xc.getKey());
                        if (c == null) {
                            c = new HashMap<>();
                            ec.put(xc.getKey(), c);
                        }
                        c.putAll(xc.getValue());
                    }));

            extractValueFromOption(cl, OPT_FELIX_FRAMEWORK_VERSION)
                    .ifPresent(config::setFrameworkVersion);

            extractValueFromOption(cl, OPT_OSGI_FRAMEWORK_ARTIFACT)
                    .ifPresent(config::setFrameworkArtifact);

        } catch (final ParseException pe) {
            Main.LOG().error("Unable to parse command line: {}", pe.getMessage(), pe);

            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("launcher", options);

            System.exit(1);
        }
    }

    public static void main(final String[] args) {

        // setup logging
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
        System.setProperty("org.slf4j.simpleLogger.showThreadName", "false");
        System.setProperty("org.slf4j.simpleLogger.levelInBrackets", "true");
        System.setProperty("org.slf4j.simpleLogger.showLogName", "false");

        // check if launcher has already been created
        final LauncherConfig launcherConfig = new LauncherConfig();
        parseArgs(launcherConfig, args);

        final Bootstrap bootstrap = new Bootstrap(launcherConfig, Main.LOG());
        bootstrap.run();
    }
}
