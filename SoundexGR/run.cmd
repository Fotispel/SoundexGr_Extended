@echo off
cd /d %~dp0

echo Building project...
call mvn -q -DskipTests package
call mvn -q dependency:copy-dependencies -DoutputDirectory=target\dependency

echo Running application...
java -Dfile.encoding=UTF-8 ^
 -cp "target\classes;target\dependency\*;src\libs\Stemmer.jar" ^
 client.GUI
