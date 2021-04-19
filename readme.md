[![Apache Sling](https://sling.apache.org/res/logos/sling.png)](https://sling.apache.org)

&#32;[![Build Status](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-feature-launcher/job/master/badge/icon)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-feature-launcher/job/master/)&#32;[![Test Status](https://img.shields.io/jenkins/tests.svg?jobUrl=https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-feature-launcher/job/master/)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-feature-launcher/job/master/test/?width=800&height=600)&#32;[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-org-apache-sling-feature-launcher&metric=coverage)](https://sonarcloud.io/dashboard?id=apache_sling-org-apache-sling-feature-launcher)&#32;[![Sonarcloud Status](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-org-apache-sling-feature-launcher&metric=alert_status)](https://sonarcloud.io/dashboard?id=apache_sling-org-apache-sling-feature-launcher)&#32;[![JavaDoc](https://www.javadoc.io/badge/org.apache.sling/org.apache.sling.feature.launcher.svg)](https://www.javadoc.io/doc/org.apache.sling/org-apache-sling-feature-launcher)&#32;[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.sling/org.apache.sling.feature.launcher/badge.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.sling%22%20a%3A%22org.apache.sling.feature.launcher%22)&#32;[![feature](https://sling.apache.org/badges/group-feature.svg)](https://github.com/apache/sling-aggregator/blob/master/docs/group/feature.md) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# Feature Model Launcher

The Feature Model Launcher can launch an feature model application file to a running process.

The launcher can be executed as follows:

```
java org.apache.sling.feature.launcher.impl.Main
```

or via the Java Main class of the jar file:
```
java -jar org.apache.sling.feature.launcher.jar
```

The launcher creates a local cache, by default in a subdirectory called `launcher`. If you want to run the launcher with a clean start, delete this directory before invoking the launcher.

The following command line options are supported:
```
$ rm -rf launcher && java -jar org.apache.sling.feature.launcher.jar -h
usage: launcher
 -C,--artifact-clash <arg>             Set artifact clash override
 -c,--cache_dir <arg>                  Set cache dir
 -CC,--config-clash <arg>              Set config clash override
 -cenv                                 print additional help information
                                       for container env vars.
 -D,--framework-properties <arg>       Set framework properties
 -ec,--extension_configuration <arg>   Provide extension configuration,
                                       format:
                                       extensionName:key1=val1,key2=val2
 -f,--feature-files <arg>              Set feature files
 -fa,--osgi-framework-artifact <arg>   Set OSGi framework artifact
                                       (overrides Apache Felix framework
                                       version)
 -fv,--felix-framework-version <arg>   Set Apache Felix framework version
                                       (default 7.0.0)
 -p,--home_dir <arg>                   Set home dir
 -u,--repository-urls <arg>            Set repository urls
 -V,--variable-values <arg>            Set variable values
 -v,--verbose <arg>                    Verbose
```

**Note**: if feature files are provided as a Classloader Resource like in an
executable JAR file or classpath resource then it's **Resource URL** can be handed
over to the Launcher as feature file (-f option):
```
java.net.URL url = getClass().getResource("/my-feature-file.json");
String[] arguments = new String[] {
    "-f", url.toString()
};
org.apache.sling.feature.launcher.impl.Main.main(arguments);
```

# Container

The container-image `apache/sling-org-apache-sling-feature-launcher:latest` is avaiable on DockerHub.

Available tags:
- **latest**    - latest build, could be a `snapshot` **or** `release` version.
- **snapshot**  - latest build of a `*-SNAPSHOT` version.
- **stable**    - latest build of a **release** version.
- **(version)** - latest build of a fixed version, could be a `snapshot - (1.1.9-SNAPSHOT)` **or** `release - (1.1.9)` version.
- **(sha1)**    - uses SHA1 hash of the git commit as tag


If you are running sling-feature-launcher as an container please use env vars.
```
$docker run -it --rm --env FEATURE_FILES=https://path.to/feature-model.json apache/sling-org-apache-sling-feature-launcher:latest

 cli-arg  -  container ENV variable
-------------------------------------
 -C       -  ARTIFACT_CLASH
 -CC      -  CONFIG_CLASH
 -c       -  CACHE_DIR
 -D       -  FRAMEWORK_PROPERTIES
 -f       -  FEATURE_FILES
 -p       -  HOME_DIR
 -u       -  REPOSITORY_URLS
 -V       -  VARIABLE_VALUES
 -ec      -  EXTENSION_CONFIGURATION
 -fv      -  FELIX_FRAMEWORK_VERSION
 -fa      -  OSGI_FRAMEWORK_ARTIFACT
 -v       -  VERBOSE
```

For further documentation see: https://github.com/apache/sling-org-apache-sling-feature/blob/master/readme.md
