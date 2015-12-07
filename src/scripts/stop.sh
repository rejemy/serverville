#!/bin/bash

pidfile=serverville.pid

if [ -e $pidfile ]; then
pid=`cat $pidfile`
echo "Stopping server ${pid}"
kill $pid
rm -f $pidfile
else
echo "Server not running"
fi
