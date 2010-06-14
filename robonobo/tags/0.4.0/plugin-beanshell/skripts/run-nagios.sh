#!/bin/bash

tmpFile=/tmp/mirror-nagios-$$
$(dirname $0)/run.sh 2>$tmpFile
retCode=0
if [[ $(grep -c SUCCESS $tmpFile) > 0 ]]
then
	grep SUCCESS $tmpFile | cut "-d|" -f2
	retCode=0
elif [[ $(grep -c ERROR $tmpFile) > 0 ]]
then
	grep ERROR $tmpFile | cut "-d|" -f2
	retCode=2
elif [[ $(grep WARNING $tmpFile) > 0 ]]
then
	grep WARNING $tmpFile | cut "-d|" -f2
	retCode=1
else
	head -1 $tmpFile
	retCode=3
fi
rm $tmpFile
exit $retCode

