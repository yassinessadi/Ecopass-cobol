
#!/usr/bin/env bash

set -euo pipefail

# ============================================================
# Environment configuration local to build jar with version >
# ============================================================

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

export JAVA_HOME=/usr/lib/jvm/temurin-11-jdk
export SPARK_HOME=/home/e7/spark-3.3.3

export PATH="$JAVA_HOME/bin:$SPARK_HOME/bin:$PATH"

export PYSPARK_PYTHON="$PROJECT_DIR/.venv/bin/python"
export PYSPARK_DRIVER_PYTHON="$PROJECT_DIR/.venv/bin/python"

# ============================================================
# Validate environment
# ============================================================

echo "=========================================="
echo " EcoPass Scala Build"
echo "=========================================="

if [[ ! -d "$JAVA_HOME" ]]; then
    echo "ERROR: JAVA_HOME does not exist: $JAVA_HOME"
    exit 1
fi

if [[ ! -d "$SPARK_HOME" ]]; then
    echo "ERROR: SPARK_HOME does not exist: $SPARK_HOME"
    exit 1
fi

if ! command -v sbt >/dev/null 2>&1; then
    echo "ERROR: sbt is not installed or not in PATH"
    exit 1
fi

echo ""
echo "Java:"
java -version

echo ""
echo "Spark:"
"$SPARK_HOME/bin/spark-submit" --version

echo ""
echo "SBT:"
sbt --version

# ============================================================
# Build
# ============================================================

cd "$PROJECT_DIR"

echo ""
echo "=========================================="
echo " Cleaning project"
echo "=========================================="

sbt test

sbt clean

echo ""
echo "=========================================="
echo " Compiling Scala sources"
echo "=========================================="

sbt compile

echo ""
echo "=========================================="
echo " Packaging JAR"
echo "=========================================="

sbt package

echo ""
echo "=========================================="
echo " Build completed successfully"
echo "=========================================="

find target/scala-2.12 -maxdepth 1 -name "*.jar" -print
