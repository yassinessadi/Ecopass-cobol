
#!/usr/bin/env bash


# """
# Script: $.py

# Description:
#     Generates a synthetic Micro Focus-style binary test file
#     using the EcoPass 2-byte record header format.

# Author:
#     jane

# Maintained by:
#     jane
# """

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

export JAVA_HOME=/usr/lib/jvm/temurin-11-jdk
export SPARK_HOME=/home/e7/spark-3.3.3

export PATH="$JAVA_HOME/bin:$SPARK_HOME/bin:$PATH"

export PYSPARK_PYTHON="$PROJECT_DIR/.venv/bin/python"
export PYSPARK_DRIVER_PYTHON="$PROJECT_DIR/.venv/bin/python"

echo "=========================================="
echo " EcoPass Scala Build"
echo "=========================================="

# Validate environment
[[ -d "$JAVA_HOME" ]] || {
    echo "ERROR: JAVA_HOME does not exist: $JAVA_HOME"
    exit 1
}

[[ -d "$SPARK_HOME" ]] || {
    echo "ERROR: SPARK_HOME does not exist: $SPARK_HOME"
    exit 1
}

command -v sbt >/dev/null 2>&1 || {
    echo "ERROR: sbt is not installed"
    exit 1
}

cd "$PROJECT_DIR"

echo ""
echo "Java:"
java -version

echo ""
echo "Spark:"
"$SPARK_HOME/bin/spark-submit" --version

echo ""
echo "SBT:"
sbt --version

echo ""
echo "=========================================="
echo " Cleaning project"
echo "=========================================="

sbt clean

echo ""
echo "=========================================="
echo " Compiling Scala sources"
echo "=========================================="

sbt compile

echo ""
echo "=========================================="
echo " Running tests"
echo "=========================================="

sbt test

echo ""
echo "=========================================="
echo " Packaging JAR"
echo "=========================================="

sbt package

echo ""
echo "=========================================="
echo " Verifying JAR"
echo "=========================================="

JAR_FILE="$(find target/scala-2.12 -maxdepth 1 -name '*.jar' | head -n 1)"

if [[ -z "$JAR_FILE" ]]; then
    echo "ERROR: JAR was not generated"
    exit 1
fi

echo "JAR: $JAR_FILE"

echo ""
echo "Checking Spark provider registration:"

jar tf "$JAR_FILE" | grep \
    'META-INF/services/org.apache.spark.sql.sources.DataSourceRegister'

echo ""
echo "Checking EcoPass DataSource class:"

jar tf "$JAR_FILE" | grep \
    'io/github/yassinessadi/ecopass/spark/EcoPassDataSource.class'

echo ""
echo "=========================================="
echo " Build completed successfully"
echo "=========================================="
