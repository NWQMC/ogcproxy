#!/usr/bin/env bash
set -e

# Read environment variables from file and export them.
file_env() {
	while read -r line || [[ -n ${line} ]]; do
		export ${line}
	done < "$1"
}

# MOUNT_DIRECTORY specifies where a volume containing the environment variables is mounted 
# into the container.
if  ! [ -z ${MOUNT_DIRECTORY} ]; then
	# if mount exists then export environment variables
	for FILE in ${MOUNT_DIRECTORY}/*.env; do
		file_env ${FILE}
	done
fi

# this is here to execute the entrypoint.sh that comes with the base image
# the entrypoint.sh contains the necessary shell commands to run the jar
/home/spring/entrypoint.sh 

exec "$@"
