# Serverville
## Overview
Extensible Java-based server for games and realtime web applications

Serverville is an easy to setup self-contained java server that provides many backend features commonly needed by games, mobile applications and web services. The main features are:

* Authentication and session management
* Robust admin APIs
* Rich key-value data storage
* Virtual currencies
* Extensible via embedded javascript/typescript or external server processes
* Client-to-client realtime communication

## Requirements
Serverville requires Java 8 or later to run. It currently also requires a MySQL database

## Building

Building Serverville requires [Apache Ant](http://ant.apache.org/) (So oldschool! How antique!)

In the root directory, type:
`./build.sh`

This will invoke Ant to build the Serverville jar and place it and all dependencies in ./build

## Setup
Before Serverville is run, a MySQL schema must be created for it. To do this, create and empty schema and run ./src/sql/Schema.sql into it.

Next you must setup your serverville.properies and log4j2.xml config files. Copy the contents of ./sample-config to wherever you want Serverville's working directory to be, and edit serverville.properties to fill in your MySQL schema URL, username and password.

## Running

From your Serverville working directory, run `java -jar serverville.jar` and the server should either start up or tell you why it couldn't. In addition, runscripts are available in ./src/scripts to provide more robust examples of how to start and stop Serverville.

## Roadmap

Coming someday to Serverville:

* Documentation!
* Clustering support
* Sharded MySQL support
* Durable caching layer to reduce database load
* Inventory support
* Admin website
* Remote javascript debugging
