@echo off
call ..\..\def.bat
"%JAVA_HOME%\bin\java.exe" com.persist.xact.x %1 %2 %3 %4 %5 %6 scriptDir=%SCRIPTS% logDir=%LOGS% docOut=%DOC_OUT% charSet="UTF-8"
