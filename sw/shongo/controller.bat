@echo off
echo %~dp0
java -jar "%~dp0controller\target\controller-1.0.jar" %*
