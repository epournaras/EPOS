#!/bin/bash

# includes
. colors.sh

# constants
eclipse_build_path="target/classes"
echo "eclipse_build_path : $eclipse_build_path"

netbeans_build_path="build/classes"
echo "netbeans_build_path : $netbeans_build_path"

class_path_verified=0

# functions
function verify_class_path
{
	class_path_verified=1
	missing_component=""
	
	echo
	class_path=$1 
	
	echo "verifying class paths:"
	
	for path in `echo $class_path | tr ':' ' '`; do
		
		echo -n "$path..."
		if [ ! -e $path ]; then red "path $path not found"; class_path_verified=0; missing_component=$path; fi
		echo "ok"
		
	done

	if [ $class_path_verified == 1 ]; then
		green "class path: OK"
	else
		orange "class path: missing at least one component: $missing_component"
	fi
	
}
