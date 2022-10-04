@echo off 
@chcp 65001
:start
for /f %%i in ('dir *.jar /ON /b') do set "latest=%%i"

echo Find latest find in folder: "%latest%"

java -Xms64M -Xmx1024M -jar %latest%

echo 現在可按視窗右上角的 X 關閉 否則將於5秒後自動重新啟動
timeout 5 > NUL

goto start