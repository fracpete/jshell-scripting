#!/bin/bash

BASEDIR=`dirname $0`/..
BASEDIR=`(cd "$BASEDIR"; pwd)`
MEMORY=512m

java -Xmx$MEMORY -cp "$BASEDIR/lib/*" com.github.fracpete.jshell.JShellPanel
