@echo off
taskkill /im kling-printer.exe /f
if exist build (
    rmdir /s /q build
)
mkdir build
cd build

cmake -G "Visual Studio 17 2022" -A x64 ..
cmake --build .
..\windeployqt6.exe --debug Debug\kling-printer.exe
mkdir Debug\plugins
move Debug\imageformats Debuplugins\
cd ..