@echo off
set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9-bin"
set "JAVA_EXEC=java"
if defined JAVA_HOME set "JAVA_EXEC=%JAVA_HOME%\bin\java"
set "MAVEN_OPTS=-Xmx1024m -XX:MaxMetaspaceSize=256m"
"%JAVA_EXEC%" -classpath "%~dp0.mvn\wrapper\maven-wrapper.jar" -Dmaven.multiModuleProjectDirectory="%~dp0" -Dwrapper.url="https://repo1.maven.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar" -Dwrapper.distributionUrl="https://repo1.maven.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip" org.apache.maven.wrapper.MavenWrapperMain %*
