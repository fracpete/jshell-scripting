@echo off

set BASEDIR=%~dp0\..
set MEMORY=512m

java -Xmx%MEMORY% -cp "%BASEDIR%/lib/*" com.github.fracpete.jshell.JShellPanel
