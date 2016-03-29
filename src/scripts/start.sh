#!/bin/bash

javabin=/usr/local/java/jdk1.8.0_71/bin/java
pidfile=serverville.pid
logdir=data/logs
jarfile=serverville.jar
debugsuspend=n
javaopts=""

if [ -e launchconfig.sh ]; then
source launchconfig.sh
fi

if [ -e $pidfile ]; then
./stop.sh
sleep 1
fi

if [ -n "$heap" ]; then
	javaopts="$javaopts -Xms$heap -Xmx$heap"
fi

if [ -n "$profileport" ]; then
	javaopts="$javaopts -agentpath:$profilelib=port=$profileport"
fi

if [ -n "$jmxport" ]; then  
   chmod 600 $jmxpassfile $jmxaccessfile
   javaopts="$javaopts -Dcom.sun.management.jmxremote.port=$jmxport -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.password.file=$jmxpassfile -Dcom.sun.management.jmxremote.access.file=$jmxaccessfile"
fi

if [ -n "$debugport" ]; then  
   javaopts="$javaopts -Xdebug -Xrunjdwp:transport=dt_socket,address=$debugport,server=y,suspend=$debugsuspend"
fi

rm -Rf $logdir
mkdir -p $logdir

finalcommand="$javabin $javaopts -jar $jarfile"

echo "Starting server"
echo $finalcommand

nohup $finalcommand > $logdir/consoleout.log 2>&1 &

