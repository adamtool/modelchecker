#!/bin/bash
# @author Manuel Gieseking
IFS=',' read -r -a dep_folders <<< "$1"
IFS=',' read -r -a dep_repos <<< "$2"
PREFIX="dependencies"

if [ ! -d "$PREFIX" ]; then 
	echo "Creating the folder '$PREFIX'."
	mkdir $PREFIX
fi
cd $PREFIX

count=0
for dep in "${dep_folders[@]}"	# all dependencies
	do	
		echo "%%%%%%%%%%%%%%%% DEPENDENCY: $dep"
		if [ -d "$dep" ]; then 
            cd $dep
			echo "Start pulling the git repository ${dep_repos[$count]}"
			git pull
            cd ..
		else 
			# The dependency is missing checkout the corresponding repo
			echo "The dependency '$PREFIX/$dep' does not exist."			
			echo "Start cloning the git repository ${dep_repos[$count]}"
			git clone ${dep_repos[$count]}
		fi
	    count=$(($count+1));		
done

cd ..
