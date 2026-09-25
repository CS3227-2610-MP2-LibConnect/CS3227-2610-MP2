#!/usr/bin/env bash

set -euo pipefail

project_directory="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_directory"

if [[ -x "$project_directory/mvnw" ]]; then
    maven_command=("$project_directory/mvnw")
elif command -v mvn >/dev/null 2>&1; then
    maven_command=(mvn)
else
    echo "Maven was not found. Install Maven or add the Maven wrapper to the project." >&2
    exit 1
fi

if command -v xvfb-run >/dev/null 2>&1; then
    exec xvfb-run --auto-servernum "${maven_command[@]}" -Pui-tests test
fi

echo "xvfb-run was not found; running JavaFX tests on the current display."
exec "${maven_command[@]}" -Pui-tests test
