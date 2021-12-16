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
import java.io.PrintWriter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

    public static final String OPT_PRINT_CONTAINER_ENV_HELP = "cenv";

    private static Logger LOGGER;

    private static Options options;

    private static final List<String> logLevels = Arrays.asList("trace", "debug", "info", "warn",
            "error", "off");
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

    static Map.Entry<String, Map<String, String>> splitMap2(final String val) {

        String[] split1 = val.split(":");

        if (split1.length < 2) {
            return new AbstractMap.SimpleEntry<>(split1[0], Collections.emptyMap());
        }

        Map<String, String> m = splitMap(split1[1]);

        return new AbstractMap.SimpleEntry<>(split1[0], m);
    }

    private static Map<String, String> splitMap(String value) {
        Map<String, String> m = new HashMap<>();
        for (String kv : value.split(",")) {
            String[] keyval = splitKeyVal(kv);
            m.put(keyval[0], keyval[1]);
        }
        return m;
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
     * @param config Configuration object
     * @return Options object.
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
                .longOpt("framework-property")
                .desc("Set framework property, format: -D key1=val1 -D key2=val2")
                .hasArg()
                .optionalArg(true)
                .build();

        final Option varValue = Option.builder(OPT_VARIABLE_VALUES)
                .longOpt("variable-value")
                .desc("Set variable value, format: -V key1=val1 -V key2=val2")
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

        final Option printInsideContainerHelp = Option.builder(OPT_PRINT_CONTAINER_ENV_HELP)
              
                .desc("print additional help information for container env vars.")
                .optionalArg(true)
                .build();
        
               options = new Options().addOption(artifactClashOverride)
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
                .addOption(frameworkArtifactOption)
                .addOption(printInsideContainerHelp);

        
        final CommandLineParser clp = new DefaultParser();
        try {
            final CommandLine cl = clp.parse(options, args);

            extractValuesFromOption(cl, OPT_REPOSITORY_URLS).ifPresent(
                    values -> config.setRepositoryUrls(values.stream().toArray(String[]::new)));

            extractValuesFromOption(cl, OPT_ARTICACT_CLASH).ifPresent(values -> values
                    .forEach(v -> config.getArtifactClashOverrides().add(ArtifactId.parse(v))));

            extractValuesFromOption(cl, OPT_CONFIG_CLASH).orElseGet(ArrayList::new)
            .forEach(value -> {
                final String[] keyVal = split(value);
                config.getConfigClashOverrides().put(keyVal[0], keyVal[1]);
            });

            extractValuesFromOption(cl, OPT_FRAMEWORK_PROPERTIES).orElseGet(ArrayList::new)
            .forEach(value -> {
                final String[] keyVal = split(value);
                config.getInstallation().getFrameworkProperties().put(keyVal[0], keyVal[1]);
            });

            extractValuesFromOption(cl, OPT_VARIABLE_VALUES).orElseGet(ArrayList::new)
            .forEach(value -> {
                final String[] keyVal = split(value);
                config.getVariables().put(keyVal[0], keyVal[1]);
            });

            if (cl.hasOption(OPT_VERBOSE)) {
                extractValueFromOption(cl, OPT_VERBOSE, "debug").ifPresent(value -> {

                    if (isLoglevel(value)) {
                        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", value);
                    }
                });
            } // if not the `org.slf4j.simpleLogger.defaultLogLevel` is by default `info`

            extractValuesFromOption(cl, OPT_FEATURE_FILES).orElseGet(ArrayList::new)
                    .forEach(config::addFeatureFiles);

            extractValueFromOption(cl, OPT_CACHE_DIR).map(File::new)
                    .ifPresent(config::setCacheDirectory);

            extractValueFromOption(cl, OPT_HOME_DIR).map(File::new)
                    .ifPresent(config::setHomeDirectory);

            extractValuesFromOption(cl, OPT_EXTENSION_CONFIGURATION)
                    .ifPresent(values -> values.forEach(v -> {
                        Map.Entry<String, Map<String, String>> xc = splitMap2(v);
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

            printHelp();

            System.exit(1);
        }
    }

    private static boolean isLoglevel(String value) {

        return logLevels.contains(value);
    }

    static void printHelp() {

        if (options == null) {
            return;
        }
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("launcher", options);
        if (options.getOption(OPT_PRINT_CONTAINER_ENV_HELP) != null) {

            try (PrintWriter writer = new PrintWriter(System.out);) {

                writer.println("");
                writer.println(
                        "If you are running sling-feature-launcher as an container please use env vars.");
                writer.println("");

                writer.println(" cli-arg  -  container ENV variable");
                writer.println("-------------------------------------");
                writer.println(" -" + OPT_ARTICACT_CLASH + "       -  ARTIFACT_CLASH");
                writer.println(" -" + OPT_CONFIG_CLASH + "      -  CONFIG_CLASH");
                writer.println(" -" + OPT_CACHE_DIR + "       -  CACHE_DIR");
                writer.println(" -" + OPT_FRAMEWORK_PROPERTIES + "       -  FRAMEWORK_PROPERTIES format: `key1=val1` for more `key1=val1 -D key2=val2`");
                writer.println(" -" + OPT_FEATURE_FILES + "       -  FEATURE_FILES");
                writer.println(" -" + OPT_HOME_DIR + "       -  HOME_DIR");
                writer.println(" -" + OPT_REPOSITORY_URLS + "       -  REPOSITORY_URLS");
                writer.println(" -" + OPT_VARIABLE_VALUES + "       -  VARIABLE_VALUES format: `variable1=value1` for more `variable1=val1 -V variable2=val2`");
                writer.println(
                        " -" + OPT_EXTENSION_CONFIGURATION + "      -  EXTENSION_CONFIGURATION");
                writer.println(
                        " -" + OPT_FELIX_FRAMEWORK_VERSION + "      -  FELIX_FRAMEWORK_VERSION");
                writer.println(
                        " -" + OPT_OSGI_FRAMEWORK_ARTIFACT + "      -  OSGI_FRAMEWORK_ARTIFACT");
                writer.println(" -" + OPT_VERBOSE + "       -  VERBOSE {trace, debug, info, warn, error, off}");

                writer.println("");
                writer.println("Java options could be set using the env var 'JAVA_OPTS'");
                writer.flush();
            }
        }

    }

    /** Split a string into key and value */
    private static String[] split(final String val) {
        final int pos = val.indexOf('=');
        if ( pos == -1 ) {
            return new String[] {val, "true"};
        }
        return new String[] {val.substring(0, pos), val.substring(pos + 1)};
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
