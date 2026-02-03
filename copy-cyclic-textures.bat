@echo off
echo ========================================
echo 复制 Cyclic GUI 材质文件到 rsring
echo ========================================
echo.

set SOURCE=..\Cyclic-trunk-1.12\src\main\resources\assets\cyclicmagic\textures\gui
set TARGET=src\main\resources\assets\rsring\textures\gui

echo 创建目标目录...
if not exist "%TARGET%" mkdir "%TARGET%"

echo.
echo 复制必要的材质文件...

REM 背景纹理
echo [1/6] 复制背景纹理...
copy "%SOURCE%\table.png" "%TARGET%\table.png"
copy "%SOURCE%\table_plain.png" "%TARGET%\table_plain.png"

REM 槽位纹理
echo [2/6] 复制槽位纹理...
copy "%SOURCE%\inventory_slot.png" "%TARGET%\inventory_slot.png"
copy "%SOURCE%\slot_large_plain.png" "%TARGET%\slot_large_plain.png"

REM 按钮纹理
echo [3/6] 复制按钮纹理...
copy "%SOURCE%\buttons.png" "%TARGET%\buttons.png"

REM 过滤器纹理
echo [4/6] 复制过滤器纹理...
if exist "%SOURCE%\filter.png" copy "%SOURCE%\filter.png" "%TARGET%\filter.png"

REM 进度条纹理（如果需要）
echo [5/6] 复制进度条纹理...
if exist "%SOURCE%\progress.png" copy "%SOURCE%\progress.png" "%TARGET%\progress.png"

REM 能量条纹理（如果需要）
echo [6/6] 复制能量条纹理...
if exist "%SOURCE%\energy_ctr.png" copy "%SOURCE%\energy_ctr.png" "%TARGET%\energy_ctr.png"
if exist "%SOURCE%\energy_inner.png" copy "%SOURCE%\energy_inner.png" "%TARGET%\energy_inner.png"

echo.
echo ========================================
echo 材质文件复制完成！
echo.
echo 已复制的文件：
dir /b "%TARGET%"
echo ========================================
pause
