# -----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements. See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership. The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied. See the License for the
# specific language governing permissions and limitations
# under the License.
# -----------------------------------------------------------------------------

#Base
FROM adoptopenjdk/openjdk11:alpine-slim
LABEL maintainer dev@sling.apache.org

#ENV 
ENV ARTIFACT_CLASH=
ENV CONFIG_CLASH=
ENV REPOSITORY_URLS=
ENV CACHE_DIR=
ENV FRAMEWORK_PROPERTIES=
ENV FEATURE_FILES=
ENV HOME_DIR=
ENV VARIABLE_VALUES=
ENV EXTENSION_CONFIGURATION=
ENV FELIX_FRAMEWORK_VERSION=
ENV OSGI_FRAMEWORK_ARTIFACT=
ENV VERBOSE=warn


WORKDIR /opt/run
RUN addgroup -S launcher && adduser -S launcher -G launcher && \
 chown -R launcher:launcher /opt/run

# copy the packaged jar file into our docker image
COPY target/${project.artifactId}-${project.version}.jar /opt/run/org.apache.sling.feature.launcher.jar

#maybe cleanup
#RUN echo "rm -rf launcherecho "rm -rf launcher \n"

# set the startup command to execute the jar
CMD java -jar /opt/run/org.apache.sling.feature.launcher.jar -C $ARTIFACT_CLASH -CC $CONFIG_CLASH -c $CACHE_DIR -D $FRAMEWORK_PROPERTIES -f $FEATURE_FILES -p $HOME_DIR -u $REPOSITORY_URLS -V $VARIABLE_VALUES -ec $EXTENSION_CONFIGURATION -fv $FELIX_FRAMEWORK_VERSION -fa $OSGI_FRAMEWORK_ARTIFACT -v $VERBOSE
