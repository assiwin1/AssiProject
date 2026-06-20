@echo off
setlocal enabledelayedexpansion

set "outputFile=output.html"
echo ^<html^>^<body^> > "%outputFile%"

for /r %%i in (*.java) do (
	echo ^<h2^>%filename%^</h2^> >> "%outputFile%"
	echo ^<h2^>%%~ni.java^</h2^> >> "%outputFile%"
    echo ^<pre^> >> "%outputFile%"
    for /f "tokens=* delims=" %%a in ('type "%%i"') do (
        set "line=%%a"
        set "line=!line:<=^<!"
        set "line=!line:>=^>!"
        echo !line! >> "%outputFile%"
    )
    echo ^</pre^> >> "%outputFile%"
)

echo ^</body^>^</html^> >> "%outputFile%"

echo "הסקריפט סיים. קובץ ה-HTML נוצר: %outputFile%"
endlocal