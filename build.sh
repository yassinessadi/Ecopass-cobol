
#!/usr/bin/env bash

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cd "$PROJECT_DIR"

echo "=========================================="
echo " EcoPass Scala Build"
echo "=========================================="

echo "[1/3] Running tests..."
sbt clean test

echo "[2/3] Compiling..."
sbt compile

echo "[3/3] Packaging JAR..."
sbt package

echo "=========================================="
echo " Build completed successfully"
echo "=========================================="

find target/scala-2.12 -maxdepth 1 -name "*.jar" -print
