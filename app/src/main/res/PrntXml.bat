@echo off
setlocal enabledelayedexpansion

set "outputFile=output.txt"


for /r %%i in (*.xml) do (
      
      
        for /f "tokens=* delims=" %%a in ('type "%%i"') do (
                set "line=%%a"
                set "line=!line:<=<!"
                set "line=!line:>=>!"
                echo !line! >> "%outputFile%"
        )
     
)



echo "הסקריפט סיים. קובץ ה-HTML נוצר: %outputFile%"
endlocal