#!/bin/bash

CLASSPATH=/robo/testnode/robonobo.jar:/robo/testnode/plugin-beanshell.jar
ROBOHOME=/robo/testnode/home
export CLASSPATH ROBOHOME

(
	sleep 10
	echo "bsh /robo/testnode/testmirror.bsh /robo/testnode/test.mp3 md5:78e0f23fdd479bb534d5514a447d297d 120"
) | java com.robonobo.gui.RobonoboFrame >/dev/null

 
