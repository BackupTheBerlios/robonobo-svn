#!/bin/bash

CLASSPATH=../target/classes:../../wang-client/target/classes:../WebContent/WEB-INF/lib/spring.jar:../../common/lib/log4j-1.2.13.jar:../../common/lib/commons-logging-1.0.3.jar:../../common-hibernate/lib/postgresql-8.3-603.jdbc3.jar:../../common/lib/jdom1.1.jar
#DEBUG="-Xdebug -Xrunjdwp:transport=dt_socket,address=7000,server=y,suspend=y"
java $DEBUG -cp $CLASSPATH com.robonobo.wang.server.utils.CreateDenoms $*

