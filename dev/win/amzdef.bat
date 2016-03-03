@echo off
set XACT=c:\nestor\xact
set JAVA_HOME=c:\program files\java\jdk1.5.0_06
set ANT_HOME=c:\nestor\download\apache-ant-1.6.5
set BDB_JAR=c:\program files\Sleepycat Software\Berkeley DB 4.3.28\jar\db.jar
set SERVLET_JAR=c:\nestor\download\jakarta-tomcat-5.5.9\common\lib\servlet-api.jar

set LOGS=%XACT%\var\logs
set WEBLOGS=%XACT%\var\weblogs
set SCRIPTS=%XACT%\dev\scripts
set SOURCE=%XACT%\dev\source
set OBJECT=%XACT%\var\object
set JAR=%XACT%\var\xact.jar
set TOOLS_JAR=%JAVA_HOME%\lib\tools.jar
set CLASSPATH=%JAR%;%TOOLS_JAR%;%BDB_JAR%;%SERVLET_JAR%
set DOC_OUT=%XACT%\var\doc