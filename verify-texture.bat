@echo off
echo ========================================
echo 经验泵控制器材质验证脚本
echo ========================================
echo.

echo [1/4] 检查材质文件...
if exist "src\main\resources\assets\rsring\textures\items\experience_pump_controller.png" (
    echo ✓ 材质文件存在
    dir "src\main\resources\assets\rsring\textures\items\experience_pump_controller.png" | findstr "experience_pump_controller.png"
) else (
    echo ✗ 材质文件不存在！
    goto :error
)
echo.

echo [2/4] 检查模型文件...
if exist "src\main\resources\assets\rsring\models\item\experience_pump_controller.json" (
    echo ✓ 模型文件存在
) else (
    echo ✗ 模型文件不存在！
    goto :error
)
echo.

echo [3/4] 检查 JAR 文件...
if exist "build\libs\rsring-1.0.jar" (
    echo ✓ JAR 文件存在
    echo.
    echo 检查 JAR 内容...
    jar tf build\libs\rsring-1.0.jar | findstr experience_pump_controller
    if errorlevel 1 (
        echo ✗ JAR 中未找到材质文件！
        echo 请运行: gradlew clean build
        goto :error
    ) else (
        echo ✓ JAR 中包含材质文件
    )
) else (
    echo ✗ JAR 文件不存在！
    echo 请运行: gradlew build
    goto :error
)
echo.

echo [4/4] 验证完成
echo ========================================
echo 所有检查通过！
echo.
echo 下一步操作：
echo 1. 运行游戏: gradlew runClient
echo 2. 在游戏中查看经验泵控制器材质
echo 3. 如果材质仍未显示，按 F3+T 重新加载资源
echo ========================================
goto :end

:error
echo.
echo ========================================
echo 验证失败！请检查上述错误信息。
echo ========================================
pause
exit /b 1

:end
pause
