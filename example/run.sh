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


export JAVA_HOME=/usr/lib/jvm/temurin-11-jdk
export SPARK_HOME=/home/e7/spark-3.3.3

export PATH="$JAVA_HOME/bin:$SPARK_HOME/bin:$PATH"

export PYSPARK_PYTHON="$/home/e7/Public/cobol-parser/cobrix-pyspark/.venv/bin/python"
export PYSPARK_DRIVER_PYTHON="/home/e7/Public/cobol-parser/cobrix-pyspark/.venv/bin/python"

$SPARK_HOME/bin/spark-submit --version


echo "=== Java version ==="
java -version

echo "=== Test JAR with spark-submit ==="

"$SPARK_HOME/bin/spark-submit" \
    --master local[*] \
    --jars "$(realpath target/scala-2.12/ecopass_2.12-0.1.0.jar)" \
    ./example/native_test.py
    # ./example/generate_index.py

echo "=== Test completed ==="