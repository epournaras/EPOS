#!/bin/bash

# arguments
deployment=$1		# name of an existing path that contains a conf folder with the required configuration settings, e.g deployments/localhost  

# includes
. colors.sh

# verify arguments
if [ -z $deployment ]; then red "missing argument 1 : deployment"; exit 1; fi
	
echo "deployment : $deployment"

# constants
links="target/classes lib"
echo "links : $links"

# files + folders
if [ ! -e $deployment ]; then echo "deployment $deployment not found"; exit 1; fi

cd $deployment

# copy links
for link in $links; do

	target=../../$link
	if [ ! -e $target ]; then echo "target $target not found"; exit 1; fi

	ln -s $target .
	
done

# done
echo "completed"
