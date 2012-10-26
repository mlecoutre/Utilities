Utilities
=========
List of small utility projects listed below.


maven-lib-deployer
==================
Description
-----------
Small utilities projects used in my job
* maven-lib-deployer: allow to deploy a folder of libs on a maven repository
* Works on Windows and Unix environment
* Will use _MAVEN\_HOME_ and _JAVA\_HOME_ environment variables

Usage
-----
* At this, you need to fork the project
* Edit the _Deployer.java_ class
* Update the three static attributes
> 1. _DESTINATION\_VERSION_
> 2. _DESTINATION\_GROUPID_ 
> 3. _LOCAL\_LIBRARY\_PATH_

Improvements
------------
* In the main method, create a command line interface, maube using commons-cli
* Maybe add a maven plugin interface  or javafx?

Links
-----