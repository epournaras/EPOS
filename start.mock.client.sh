#!/bin/bash

# launch test client for the PostgreSQL persistence daemon

# usage:

# ./start.mock.client.sh
# ./start.mock.client.sh port

# arguments
# 1. daemon list port; optional, default is defaultServerPort


# eag - 2017-11-26

# defaults
defaultServerPort=5433
defaultSleepTime=5000	# milliseconds

# arguments
serverPort=$1
use_screen=$2
sleep_time_ms=$3

# includes
. colors.sh
. verify.class.path.sh

# flags
set -e
set -u

# verify arguments
if [ -z $serverPort ]; then orange "missing argument 1 : serverPort -> $defaultServerPort"; serverPort=$defaultServerPort; fi
if [ -z $use_screen ]; then orange "missing argument 2 : use_screen -> 1"; use_screen=1; fi
if [ -z $sleep_time_ms ]; then orange "missing argument 3 : sleep_time_ms -> $defaultSleepTime"; sleep_time_ms=$defaultSleepTime; fi
	
echo "serverPort : $serverPort"
echo "use_screen : $use_screen"
echo "sleep_time_ms : $sleep_time_ms"

# constants
javamem=128m
echo "javamem : $javamem"

# set the java classpath, using either:
# - the compiled .class files from the source folders (typically on a dev machine)
# - the .jar files in the lib folder (typically on a deployment machine)  

classpath_src="$eclipse_build_path:../gson/$eclipse_build_path:../jeromq/$eclipse_build_path:lib/*"
#classpath_src="$eclipse_build_path:../gson/$eclipse_build_path:../jeromq3-x/$eclipse_build_path:lib/*"
echo "classpath_src : $classpath_src"

classpath_lib="$eclipse_build_path:lib/gson.jar:lib/jeromq4.jar:lib/*"
#classpath_lib="$eclipse_build_path:lib/gson.jar:lib/jeromq3.jar:lib/*"
echo "classpath_lib : $classpath_lib"

screenname="pg.client"
echo "screenname : $screenname"

logdir=log
echo "logdir : $logdir"

logfile=$logdir/client.log
echo "logfile : $logfile"

# verify bootstrap server not already running
if [ $use_screen == 1 ]; then
	if screen -ls | grep "$screenname" ; then 
			red "a screen session with name $screenname already found -> goodbye"
			exit 1 
	fi
fi



# files + folders
mkdir -p $logdir
rm -f $logfile


# verify java class path
verify_class_path "$classpath_src"
echo "class_path_verified : $class_path_verified"

if [ $class_path_verified == 1 ]; then
	classpath=$classpath_src
	echo "using source classpath"
	
else
	echo "trying alternative -> verifying lib classpath"
	verify_class_path "$classpath_lib"
	echo "class_path_verified : $class_path_verified"


	if [ $class_path_verified == 1 ]; then
		classpath=$classpath_lib
		echo "using lib classpath"
	else
		red "no classpath available (either source or lib)"
		exit 1
	fi
fi

echo
echo "classpath : $classpath"
echo

	
# launch in a screen window
title="daemon"
echo "title : $title"

script=/tmp/$0.sh
echo "java -Xmx$javamem -cp $classpath pgpersist.MockClient $serverPort $sleep_time_ms | tee -a $logfile" > $script
	
echo -n "launching mock client on port $serverPort with script $script and $javamem..."

if [ $use_screen == 1 ]; then
	screen -d -m -S $screenname -t "$title" bash $script
	
	# login
	screen -dr $screenname

else
	bash $script
fi


echo
echo launched
	

