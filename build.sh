#!/bin/bash
set -e
./webbuild.sh
ant clean jar
