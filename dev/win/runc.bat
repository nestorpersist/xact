rem @echo off
"%JAVA_HOME%\bin\java.exe" com.persist.xact.x "%QUERY_STRING%" mime="text/html" asciiXML funcOnly REQUEST_METHOD=%REQUEST_METHOD% REMOTE_ADDR=%REMOTE_ADDR% debugError=false scriptDir=%SCRIPTS% logDir=%WEBLOGS%
