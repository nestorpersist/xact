call ..\..\def.bat
"%JAVA_HOME%\bin\javadoc.exe" -private -d %XACT%\var\javadoc -sourcepath %SOURCE% -subpackages com.persist -overview %SOURCE%\overview.html com.persist.xact com.persist.xdom com.persist.xenv
