#!/bin/bash

# launch PostgreSQL persistence daemon

# usage:

# ./start.daemon.sh
# ./start.daemon.sh port

# arguments
# 1. daemon list port; optional, default is defaultServerPort


# eag - 2017-11-26

# constants


# arguments
deployment=$1		# name of an existing path that contains a conf folder with the required configuration settings, e.g deployments/localhost
use_screen=$2



# includes
. colors.sh
. verify.class.path.sh

# flags
set -e
set -u

# verify arguments
if [ -z $deployment ]; then red "missing argument 1 : deployment"; exit 1; fi
if [ -z $use_screen ]; then orange "missing argument 2 : use_screen -> 1"; use_screen=1; fi

echo "deployment : $deployment"
echo "use_screen : $use_screen"


# constants
deployment_name=`echo $deployment | sed "s#deployments/##"`
echo "deployment_name : $deployment_name"

config=$deployment/conf/daemon.conf
echo "config : $config"

commit_rate=1000
echo "commit_rate : $commit_rate"

commit_period_seconds=10
echo "commit_period_seconds : $commit_period_seconds"

javamem=16384m
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

screenname="pg.daemon"."$deployment_name"
echo "screenname : $screenname"

logdir=log
echo "logdir : $logdir"

logfile=$logdir/server.log
echo "logfile : $logfile"

# verify bootstrap server not already running
if [ $use_screen == 1 ]; then
	if screen -ls | grep "$screenname" ; then 
			red "a screen session with name $screenname already found -> goodbye"
			exit 1 
	fi
fi



# files + folders
if [ ! -e $deployment ]; then red "deployment $deployment not found"; exit 1; fi
if [ ! -e $config ]; then red "config $config not found"; exit 1; fi
	
# get listen host and port
# add edward | 2018-08-23
serverHost=`cat $config | grep "listenIP" | cut -f2 -d'=' | tr -d ' '`
serverPort=`cat $config | grep "listenPort" | cut -f2 -d'=' | tr -d ' '`
database=`cat $config | grep "database" | cut -f2 -d'=' | tr -d ' '`

echo "serverHost : $serverHost"	
echo "serverPort : $serverPort"
echo "database : $database"

if [ -z $serverHost ]; then red "could not find listenIP in $config"; exit 1; fi
if [ -z $serverPort ]; then red "could not find listenPort in $config"; exit 1; fi
if [ -z $database ]; then red "could not find database in $config"; exit 1; fi

# setup the deployment
./setup.deployment.sh $deployment

cd $deployment

mkdir -p $logdir
rm -f $logfile


# verify java class path
# verify_class_path "$classpath_src"
# echo "class_path_verified : $class_path_verified"

# if [ $class_path_verified == 1 ]; then
# 	classpath=$classpath_src
# 	echo "using source classpath"
	
# else
# 	echo "trying alternative -> verifying lib classpath"
# 	verify_class_path "$classpath_lib"
# 	echo "class_path_verified : $class_path_verified"


# 	if [ $class_path_verified == 1 ]; then
# 		classpath=$classpath_lib
# 		echo "using lib classpath"
# 	else
# 		red "no classpath available (either source or lib)"
# 		exit 1
# 	fi
# fi

# echo
# echo "classpath : $classpath"
# echo

	
# launch in a screen window
title="daemon"
echo "title : $title"

script=/tmp/$0.server.sh
echo "java -D$deployment_name -Xmx$javamem -cp target/classes:lib/gson.jar:lib/jeromq4.jar:lib/* pgpersist.PostgresPersistenceDaemon $database $serverPort $serverHost $commit_rate $commit_period_seconds | tee -a $logfile" > $script
	
echo -n "launching persistence daemon for db $database on port $serverPort with script $script and $javamem..."

if [ $use_screen == 1 ]; then
	screen -d -m -S $screenname -t "$title" bash $script
	
	# login
	screen -dr $screenname

else
	bash $script
fi


echo
echo launched
	

