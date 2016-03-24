#!/bin/bash

file="console.txt"
> $file

exec >> $file
exec 2>&1

iterateDir () {
	func=$1
	for f in $2/*
	do
		eval $1 $f
	done
}

# ~~~~~~~~~~ Fetch newest java on machine ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

echo "Searching for java....."

jvm="/usr/lib/jvm"
javaVersions=()
declare -A javaPaths
checkJava () {
	if ! [ -h "$1" ]; then # not a symlink
		if [ -d "$1/bin" ]; then # contains a bin directory
			parts=(${1//// })
			parts_length=${#parts[@]}
			parts_last_i=$((parts_length - 1))
			parts_last=${parts[${parts_last_i}]}
			file=$parts_last
			
			fileParts=(${file//-/ })
			javaVersion=${fileParts[1]}
			
			javaVersions+=("$javaVersion")
			javaPaths[$javaVersion]=$1
		fi
	fi
}

iterateDir checkJava $jvm

IFS=$'\n' sorted=($(sort -r <<<"${javaVersions[*]}"))
unset IFS

version="${sorted[0]}"
javaPath="${javaPaths[$version]}/bin/java"

echo "Java $version found"
`$javaPath -version`
echo ""

# ~~~~~~~~~~ Load CFG File ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

echo "Loading game configuration....."

source ./runConfig.cfg

echo ""

# ~~~~~~~~~~ Create server arguments ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

echo "Loading arguments....."

arguments=()
arguments+=($javaPath)
arguments+=("-server")
if (( "$ram_min" > "0" )); then
	arguments+=(-$(printf "Xms%dM" $ram_min))
fi
if (( "$ram_max" > "0" )); then
	arguments+=(-$(printf "Xmx%dM" $ram_max))
fi
if (( "$perm_gen" > "0" )); then
	arguments+=(-$(printf "XX:MaxPermSize=%dm" $perm_gen))
fi
if [ -n "$other_java_params" ]; then
	params=(${other_java_params// / })
	for param in ${params[@]}
	do
		arguments+=($param)
	done
fi
arguments+=("-jar")
arguments+=("$jar_name")
arguments+=("nogui")
if [ -n "$other_mc_params" ]; then
	params=(${other_mc_params// / })
	for param in ${params[@]}
	do
		arguments+=($param)
	done
fi

command=""
for javaArg in "${arguments[@]}"
do
	command+="$javaArg "
done

echo ""

# ~~~~~~~~~~ Start the game ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

echo "----=====##### SERVER PROCESS  STARTING #####=====-----"

echo `$command`

echo "----=====##### SERVER PROCESS HAS ENDED #####=====-----"
