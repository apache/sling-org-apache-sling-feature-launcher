[<img src="http://sling.apache.org/res/logos/sling.png"/>](http://sling.apache.org)

 [![Build Status](https://builds.apache.org/buildStatus/icon?job=sling-org-apache-sling-feature-launcher-1.8)](https://builds.apache.org/view/S-Z/view/Sling/job/sling-org-apache-sling-feature-launcher-1.8) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0) [![feature](https://sling.apache.org/badges/group-feature.svg)](https://github.com/apache/sling-aggregator/blob/master/docs/groups/feature.md)

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
 -c <arg>    Set cache dir
 -D <arg>    Set framework properties
 -f <arg>    Set feature files
 -fv <arg>   Set felix framework version
 -p <arg>    Set home dir
 -u <arg>    Set repository url
 -V <arg>    Set variable value
 -v          Verbose
```


For further documentation see: https://github.com/apache/sling-org-apache-sling-feature/blob/master/readme.md
