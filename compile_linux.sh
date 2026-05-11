#!/bin/bash
# Compile report.tex using lualatex + plantuml.jar (for Linux/sandbox)
# Requires: plantuml.jar in current directory (run compile.bat on Windows first)

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

if [ ! -f "plantuml.jar" ]; then
    echo "ERROR: plantuml.jar not found. Run compile.bat on Windows first to copy it here."
    exit 1
fi

export PLANTUML_JAR="$SCRIPT_DIR/plantuml.jar"

echo "Compiling pass 1..."
lualatex -shell-escape -interaction=nonstopmode report.tex

echo "Compiling pass 2 (cross-references)..."
lualatex -shell-escape -interaction=nonstopmode report.tex

echo "Done! report.pdf generated."
