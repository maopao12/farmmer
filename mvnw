#!/bin/sh
# Maven Wrapper startup script

# Resolve links
PRG="$0"
while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# Set MAVEN_HOME
MAVEN_HOME="$HOME/.m2/wrapper/dists/apache-maven-3.9.9-bin"

# Find Java
if [ -z "$JAVA_HOME" ]; then
  JAVA_EXEC="java"
else
  JAVA_EXEC="$JAVA_HOME/bin/java"
fi

# Set Maven opts
MAVEN_OPTS="${MAVEN_OPTS} -Xmx1024m -XX:MaxMetaspaceSize=256m"

# Run Maven via wrapper
exec "$JAVA_EXEC" \
  -classpath "$(dirname "$0")/.mvn/wrapper/maven-wrapper.jar" \
  -Dmaven.multiModuleProjectDirectory="$(dirname "$0")" \
  -Dwrapper.url="https://repo1.maven.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar" \
  -Dwrapper.distributionUrl="https://repo1.maven.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip" \
  org.apache.maven.wrapper.MavenWrapperMain \
  "$@"
