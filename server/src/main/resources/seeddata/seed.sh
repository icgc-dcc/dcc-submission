#!/bin/bash

traverse() 
{
  # Traverse a directory
  ls "$1" | while read i
  do
    if [ -d "$1/$i" ]; then
      traverse "$1/$i"
    else 
      echo "Uploading file: $1/$i"
      curl -XPOST -H "Authorization: X-DCC-Auth YWRtaW46YWRtaW5zcGFzc3dk" http://$SERVER/ws/seed/$1/$i --data-binary @$1/$i
    fi
  done
}

printusage()
{
	echo "USAGE: $0 DIRECTORY [SERVER]"
	echo "Where DIRECTORY has the following structure: "
	echo "  DIRECTORY/dictionaries.json"
	echo "  DIRECTORY/projects.json"
	echo "  DIRECTORY/releases.json"
	echo "  DIRECTORY/users.json"
	echo "  DIRECTORY/fs/[all files and subdirectories]"
	echo ""
	echo "The complete directory tree under fs will be reproduced on the remote system."
	echo "All files are optional."
	echo ""
	echo "The default value of SERVER is localhost:5380"
	exit 1
}

if [ -z "$1" ]; then
	printusage
else
	if [ -d "$1" ]; then
		SERVER=$2
		if [ -z "$2" ]; then
			SERVER="localhost:5380"
		fi
		cd $1
		for DATATYPE in users projects dictionaries releases 
		do
			if [ -e "$DATATYPE.json" ]; then
				echo "Seeding database: $DATATYPE"
				curl -XPOST -H "Authorization: X-DCC-Auth YWRtaW46YWRtaW5zcGFzc3dk" http://$SERVER/ws/seed/$DATATYPE?delete=true --data @$DATATYPE.json --header "Content-Type: application/json"
			fi
		done
	
		if [ -d "fs" ]; then
			traverse fs
		fi
	else
		echo "ERROR! Directory $1 does not exist"
		echo ""
		printusage
	fi
fi
	