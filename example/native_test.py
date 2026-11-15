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

from pyspark.sql import SparkSession
from pyspark.sql.functions import col, decode
from pathlib import Path

PROJECT_DIR = Path(__file__).resolve().parent.parent

INPUT_FILE = PROJECT_DIR / "example" / "micro_20mb.dat"
INDEX_FILE = PROJECT_DIR / "example" / "micro_20mb.epidx"
JAR_FILE = PROJECT_DIR / "target" / "scala-2.12" / "ecopass_2.12-0.1.0.jar"

spark = (
    SparkSession.builder
    .appName("EcoPassNativeTest")
    .getOrCreate()
)

try:
    df = (
        spark.read
        .format("ecopass")
        .option("headerSize", "2")
        .option("alignment", "2")
        .option("indexPath", str(INDEX_FILE))
        .load(str(INPUT_FILE))
    )

    df.printSchema()
    df.show(10, truncate=False)
    df = df.withColumn(
        "data",
        decode(col("data"), "ISO-8859-1")
    ).show(10, truncate=False)
finally:
    spark.stop()