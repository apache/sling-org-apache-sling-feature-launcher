[<img src="https://sling.apache.org/res/logos/sling.png"/>](https://sling.apache.org)

 [![Build Status](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-feature-launcher/job/master/badge/icon)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-feature-launcher/job/master/) [![Test Status](https://img.shields.io/jenkins/tests.svg?jobUrl=https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-feature-launcher/job/master/)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-feature-launcher/job/master/test/?width=800&height=600) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-org-apache-sling-feature-launcher&metric=coverage)](https://sonarcloud.io/dashboard?id=apache_sling-org-apache-sling-feature-launcher) [![Sonarcloud Status](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-org-apache-sling-feature-launcher&metric=alert_status)](https://sonarcloud.io/dashboard?id=apache_sling-org-apache-sling-feature-launcher) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.sling/org.apache.sling.feature.launcher/badge.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.sling%22%20a%3A%22org.apache.sling.feature.launcher%22) [![JavaDocs](https://www.javadoc.io/badge/org.apache.sling/org.apache.sling.feature.launcher.svg)](https://www.javadoc.io/doc/org.apache.sling/org.apache.sling.feature.launcher) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0) [![feature](https://sling.apache.org/badges/group-feature.svg)](https://github.com/apache/sling-aggregator/blob/master/docs/groups/feature.md)

# Feature Model Launcher

The Feature Model Launcher can launch an feature model application file to a running process.

The launcher can be executed as follows:

```
java org.apache.sling.feature.launcher.impl.Main
```

or via the Java Main class of the jar file:
```
java -jar org.apache.sling.feature.launcher-1.0.0.jar
```

The launcher creates a local cache, by default in a subdirectory called `launcher`. If you want to run the launcher with a clean start, delete this directory before invoking the launcher.

The following command line options are supported:
```
$ rm -rf launcher && java -jar org.apache.sling.feature.launcher-1.0.0.jar -h
usage: launcher
 -C <arg>    Set artifact clash override
 -CC <arg>   Set config class override
 -c <arg>    Set cache dir
 -D <arg>    Set framework properties
 -f <arg>    Set feature files (relative and absolute file path or URL including classloader resources)
 -fv <arg>   Set felix framework version
 -p <arg>    Set home dir
 -u <arg>    Set repository url
 -V <arg>    Set variable value
 -ec <arg>   Set Extension Configuration (format: extensionName:key1=val1,key2=val2)
 -fv <arg>   Set Felix Framework version
 -fa <arg>   Set Framework Artifact (overrides felix framework version)
 -v          Verbose
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

For further documentation see: https://github.com/apache/sling-org-apache-sling-feature/blob/master/readme.md
