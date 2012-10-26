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
* Will use MAVEN_HOME and JAVA_HOME environment variables

Usage
-----
* At this, you need to fork the project
* Edit the Deployer.java class
* Update the three static attributes
> 1. DESTINATION_VERSION
> 2. DESTINATION_GROUPID  
> 3. LOCAL_LIBRARY_PATH

Improvements
------------
* In the main method, create a command line interface, maube using commons-cli
* Maybe add a maven plugin interface  or javafx?

Links
-----