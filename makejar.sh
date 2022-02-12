#!/bin/zsh

set -ex

SPIGOT_JAR=spigot-1.17.jar
WEBSOCKET_JAR=java-websocket-1.4.0.jar
JYTHON_JAR=jython-standalone-2.7.2.jar

pushd ServerPythonInterpreterPlugin

rm -rf com/

pushd src-common/
# Compile the plugin classes
javac -cp ".:../lib-spigot/${SPIGOT_JAR}:../lib-common/${WEBSOCKET_JAR}:../lib-common/${JYTHON_JAR}" -d .. com/**/*.java
popd

jar cvf ../MinecraftPyServer.jar *.yml lib-common/ python/ com/

popd
