# """
# Script: $.py

# Description:
#     Generates a synthetic Micro Focus-style binary test file
#     using the EcoPass 2-byte record header format.

# Author:
#     e7

# Maintained by:
#     e7
# """


from pathlib import Path
from pyspark.sql import SparkSession

PROJECT_DIR = Path(__file__).resolve().parent.parent

INPUT_FILE = PROJECT_DIR / "example" / "micro_20mb.dat"
INDEX_FILE = PROJECT_DIR / "example" / "micro_20mb.epidx"

spark = (
    SparkSession.builder
    .appName("EcoPassIndexGenerator")
    .getOrCreate()
)

try:
    if not INPUT_FILE.exists():
        raise FileNotFoundError(INPUT_FILE)

    eco_pass = spark._jvm.io.github.yassinessadi.ecopass.EcoPass

    index = eco_pass.generateIndex(
        str(INPUT_FILE),
        str(INDEX_FILE)
    )

    print("EcoPass index generated successfully")
    print(f"Source: {INPUT_FILE}")
    print(f"Index: {INDEX_FILE}")
    print(f"Records: {index.totalRecords()}")
    print(f"Partitions: {index.partitions().size()}")

finally:
    spark.stop()
