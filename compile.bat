@echo off
REM === Biên dịch báo cáo LaTeX ===
set PLANTUML_JAR_PATH=C:\Users\m0tna\AppData\Local\plantuml\plantuml.jar
set PLANTUML_JAR=%CD%\plantuml.jar

REM Copy plantuml.jar vao project folder de Linux sandbox co the dung
if not exist plantuml.jar (
    echo Copying plantuml.jar to project folder...
    copy "%PLANTUML_JAR_PATH%" plantuml.jar
)

echo Compiling report.tex (pass 1)...
lualatex -shell-escape -interaction=nonstopmode report.tex

echo Compiling report.tex (pass 2 for cross-references)...
lualatex -shell-escape -interaction=nonstopmode report.tex

echo Done! Check report.pdf
pause
