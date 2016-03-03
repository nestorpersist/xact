@echo off
set XACT=c:\xact
set JAVA_HOME=c:\program files\java\jdk1.5.0_04
set ANT_HOME=c:\java\apache-ant-1.6.5
set BDB_JAR=c:\program files\Sleepycat Software\Berkeley DB 4.3.28\jar\db.jar
set SERVLET_JAR=c:\program files\apache software foundation\tomcat 5.5\common\lib\servlet-api.jar

set LOGS=%XACT%\var\logs
set WEBLOGS=%XACT%\var\weblogs
set SCRIPTS=%XACT%\dev\scripts
set SOURCE=%XACT%\dev\source
set OBJECT=%XACT%\var\object
set JAR=%XACT%\var\xact.jar
set TOOLS_JAR=%JAVA_HOME%\lib\tools.jar
set CLASSPATH=%JAR%;%TOOLS_JAR%;%BDB_JAR%;%SERVLET_JAR%
set DOC_OUT=%XACT%\var\doc