#!/bin/bash

CLASSPATH=robonobo.jar
#DEBUG_LISTEN="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9190"
JVMARGS=$DEBUG_LISTEN

export CLASSPATH

java $JVMARGS com.robonobo.Robonobo
