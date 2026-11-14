
from pyspark.sql import SparkSession

JAR_PATH = (
    "/home/e7/Public/cobol-parser/ecopass/"
    "target/scala-2.12/ecopass_2.12-0.1.0.jar"
)

INPUT_FILE = "example/spddta.a00"
OUTPUT_FILE = "example/output.txt"

spark = (
    SparkSession.builder
    .appName("EcoPassJarTest")
    .master("local[*]")
    .getOrCreate()
)

try:
    # reader = spark._jvm.za.co.absa.ecopass.RecordHeaderReader(
    # Initialize the EcoPass Scala reader
    reader = spark._jvm.io.github.yassinessadi.ecopass.RecordHeaderReader(
        2, 2, True
    )

    # Read records from the binary file
    records = reader.readRecords(INPUT_FILE)

    print(f"Records found: {records.size()}")

    # Export record payloads as text
    with open(
        OUTPUT_FILE,
        mode="w",
        encoding="iso-8859-1",
        newline="\n"
    ) as output:

        for i in range(records.size()):
            record = records.get(i)

            raw_bytes = bytes(bytearray(record.data()))

            content = raw_bytes.decode(
                "iso-8859-1",
                errors="replace"
            )

            # Replace non-printable control characters
            content = "".join(
                char if char.isprintable() or char == "\t" else "?"
                for char in content
            )

            output.write(content + "\n")

    print(f"Text file generated: {OUTPUT_FILE}")

finally:
    spark.stop()
