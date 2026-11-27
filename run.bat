@echo off
chcp 65001 >nul
cd /d "%~dp0"
java -Dfile.encoding=UTF-8 -jar dist\abac-policy-engine-1.1.0-all.jar %*
pause
