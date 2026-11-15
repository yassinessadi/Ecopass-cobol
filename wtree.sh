
#!/usr/bin/env bash


# """
# Script: wtree.sh

# Description:
#     Generates a synthetic Micro Focus-style binary test file
#     using the EcoPass 2-byte record header format.

# Author:
#     Jane

# Maintained by:
#     Jane
# """


set -euo pipefail

# Run from the EcoPass project root
BASE="src"

echo "Creating EcoPass architecture..."

# ============================================================
# MAIN SCALA SOURCE DIRECTORIES
# ============================================================

MAIN="io/github/yassinessadi/ecopass"

DIRECTORIES=(
    "$BASE/main/scala/$MAIN/core"
    "$BASE/main/scala/$MAIN/framing"
    "$BASE/main/scala/$MAIN/indexing"
    "$BASE/main/scala/$MAIN/spark"

    "$BASE/main/resources/META-INF/services"

    "$BASE/test/scala/$MAIN/core"
    "$BASE/test/scala/$MAIN/framing"
    "$BASE/test/scala/$MAIN/indexing"
    "$BASE/test/scala/$MAIN/spark"

    "$BASE/test/resources/samples"
)

for DIR in "${DIRECTORIES[@]}"; do
    mkdir -p "$DIR"
done

# ============================================================
# CORE
# ============================================================

CORE_FILES=(
    "$BASE/main/scala/$MAIN/core/RecordType.scala"
    "$BASE/main/scala/$MAIN/core/EcoPassRecord.scala"
    "$BASE/main/scala/$MAIN/core/RecordHeaderParser.scala"
)

# ============================================================
# FRAMING
# ============================================================

FRAMING_FILES=(
    "$BASE/main/scala/$MAIN/framing/RecordFramingPolicy.scala"
    "$BASE/main/scala/$MAIN/framing/HeaderFormatResolver.scala"
    "$BASE/main/scala/$MAIN/framing/PaddingResolver.scala"
    "$BASE/main/scala/$MAIN/framing/RecordBoundaryCalculator.scala"
)

# ============================================================
# INDEXING
# ============================================================

INDEXING_FILES=(
    "$BASE/main/scala/$MAIN/indexing/EcoPassIndexBuilder.scala"
    "$BASE/main/scala/$MAIN/indexing/EcoPassIndex.scala"
    "$BASE/main/scala/$MAIN/indexing/EcoPassIndexStore.scala"
    "$BASE/main/scala/$MAIN/indexing/RecordBoundary.scala"
)

# ============================================================
# SPARK DATA SOURCE V2
# ============================================================

SPARK_FILES=(
    "$BASE/main/scala/$MAIN/spark/EcoPassDataSource.scala"
    "$BASE/main/scala/$MAIN/spark/EcoPassTable.scala"
    "$BASE/main/scala/$MAIN/spark/EcoPassScanBuilder.scala"
    "$BASE/main/scala/$MAIN/spark/EcoPassScan.scala"
    "$BASE/main/scala/$MAIN/spark/EcoPassBatch.scala"
    "$BASE/main/scala/$MAIN/spark/EcoPassInputPartition.scala"
    "$BASE/main/scala/$MAIN/spark/EcoPassPartitionReaderFactory.scala"
    "$BASE/main/scala/$MAIN/spark/EcoPassPartitionReader.scala"
    "$BASE/main/scala/$MAIN/spark/EcoPassSchema.scala"
)

# ============================================================
# TEST FILES
# ============================================================

TEST_FILES=(
    "$BASE/test/scala/$MAIN/core/RecordHeaderParserSpec.scala"
    "$BASE/test/scala/$MAIN/core/RecordTypeSpec.scala"

    "$BASE/test/scala/$MAIN/framing/HeaderFormatResolverSpec.scala"
    "$BASE/test/scala/$MAIN/framing/PaddingResolverSpec.scala"
    "$BASE/test/scala/$MAIN/framing/RecordBoundaryCalculatorSpec.scala"

    "$BASE/test/scala/$MAIN/indexing/EcoPassIndexBuilderSpec.scala"

    "$BASE/test/scala/$MAIN/spark/EcoPassDataSourceSpec.scala"
    "$BASE/test/scala/$MAIN/spark/EcoPassPartitionReaderSpec.scala"
)

# ============================================================
# CREATE FILES WITHOUT OVERWRITING EXISTING CONTENT
# ============================================================

ALL_FILES=(
    "${CORE_FILES[@]}"
    "${FRAMING_FILES[@]}"
    "${INDEXING_FILES[@]}"
    "${SPARK_FILES[@]}"
    "${TEST_FILES[@]}"
)

for FILE in "${ALL_FILES[@]}"; do
    if [[ ! -e "$FILE" ]]; then
        touch "$FILE"
        echo "[CREATED] $FILE"
    else
        echo "[EXISTS]  $FILE"
    fi
done

# ============================================================
# SPARK DATASOURCE SERVICE REGISTRATION
# ============================================================

SERVICE="$BASE/main/resources/META-INF/services/org.apache.spark.sql.sources.DataSourceRegister"

if [[ ! -e "$SERVICE" ]]; then
    printf '%s\n' \
        'io.github.yassinessadi.ecopass.spark.EcoPassDataSource' \
        > "$SERVICE"

    echo "[CREATED] $SERVICE"
else
    echo "[EXISTS]  $SERVICE"
fi

# ============================================================
# DISPLAY RESULT
# ============================================================

echo
echo "========================================"
echo "EcoPass architecture created successfully"
echo "========================================"
echo

find "$BASE/main" "$BASE/test" -print | sort
