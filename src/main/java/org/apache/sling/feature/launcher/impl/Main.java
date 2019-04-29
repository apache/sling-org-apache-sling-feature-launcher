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

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the launcher main class.
 * It parses command line parameters and prepares the launcher.
 */
public class Main {

    private static Logger LOGGER;

    public static Logger LOG() {
        if ( LOGGER == null ) {
            LOGGER = LoggerFactory.getLogger("launcher");
        }
        return LOGGER;
    }

    /** Split a string into key and value */
    private static String[] split(final String val) {
        final int pos = val.indexOf('=');
        if ( pos == -1 ) {
            return new String[] {val, "true"};
        }
        return new String[] {val.substring(0, pos), val.substring(pos + 1)};
    }

    /**
     * Parse the command line parameters and update a configuration object.
     * @param args Command line parameters
     * @return Configuration object.
     */
    private static void parseArgs(final LauncherConfig config, final String[] args) {
        final Options options = new Options();

        final Option artifactClashOverride = new Option("C", true, "Set artifact clash override");
        final Option repoOption =  new Option("u", true, "Set repository url");
        final Option featureOption =  new Option("f", true, "Set feature files");
        final Option fwkProperties = new Option("D", true, "Set framework properties");
        final Option varValue = new Option("V", true, "Set variable value");
        final Option debugOption = new Option("v", false, "Verbose");
        debugOption.setArgs(0);
        final Option cacheOption = new Option("c", true, "Set cache dir");
        final Option homeOption = new Option("p", true, "Set home dir");

        final Option frameworkOption = new Option("fv", true, "Set felix framework version");

        options.addOption(artifactClashOverride);
        options.addOption(repoOption);
        options.addOption(featureOption);
        options.addOption(fwkProperties);
        options.addOption(varValue);
        options.addOption(debugOption);
        options.addOption(cacheOption);
        options.addOption(homeOption);
        options.addOption(frameworkOption);

        final CommandLineParser clp = new BasicParser();
        try {
            final CommandLine cl = clp.parse(options, args);

            if ( cl.hasOption(repoOption.getOpt()) ) {
                final String value = cl.getOptionValue(repoOption.getOpt());
                config.setRepositoryUrls(value.split(","));
            }
            if ( cl.hasOption(artifactClashOverride.getOpt()) ) {
                for(final String override : cl.getOptionValues(artifactClashOverride.getOpt())) {
                    config.getArtifactClashOverrides().add(override);
                }
            }
            if ( cl.hasOption(fwkProperties.getOpt()) ) {
                for(final String value : cl.getOptionValues(fwkProperties.getOpt())) {
                    final String[] keyVal = split(value);

                    config.getInstallation().getFrameworkProperties().put(keyVal[0], keyVal[1]);
                }
            }
            if ( cl.hasOption(varValue.getOpt()) ) {
                for(final String optVal : cl.getOptionValues(varValue.getOpt())) {
                    final String[] keyVal = split(optVal);

                    config.getVariables().put(keyVal[0], keyVal[1]);
                }
            }
            if ( cl.hasOption(debugOption.getOpt()) ) {
                System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
            }

            if ( cl.hasOption(featureOption.getOpt()) ) {
                for(final String optVal : cl.getOptionValues(featureOption.getOpt())) {
                    config.addFeatureFiles(optVal.split(","));
                }
            }
            if (cl.hasOption(cacheOption.getOpt())) {
                config.setCacheDirectory(new File(cl.getOptionValue(cacheOption.getOpt())));
            }
            if (cl.hasOption(homeOption.getOpt())) {
                config.setHomeDirectory(new File(cl.getOptionValue(homeOption.getOpt())));
            }
            if (cl.hasOption(frameworkOption.getOpt())) {
                config.setFrameworkVersion(cl.getOptionValue(frameworkOption.getOpt()));
            }
        } catch ( final ParseException pe) {
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
