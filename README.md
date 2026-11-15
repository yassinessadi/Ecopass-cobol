# EcoPass Scala Record Reader

Scala conversion of the supplied Python `RecordHeader` reader.

## Compatibility

| Component | Version |
|---|---|
| Scala | 2.12.15 |
| Apache Spark | 3.3.3 (Scala 2.12 build) |
| JDK | 11 |
| sbt | 1.10.7 |

## Build

Install an sbt 1.x launcher, then run:

```bash
sbt clean test package

# for local test use build.sh
```

The JAR is created at:

```text
target/scala-2.12/ecopass_2.12-0.2.0.jar
```

The `project/build.properties` file pins sbt 1.10.7. The sbt version is separate from the Scala and Spark versions.

## Use from PySpark

### 1. Start Spark with EcoPass

```bash
pyspark \
  --jars target/scala-2.12/ecopass_2.12-0.2.0.jar
```

Alternatively, configure the JAR when creating your Spark session:

```python
from pyspark.sql import SparkSession

spark = (
    SparkSession.builder
    .appName("EcoPassExample")
    .config(
        "spark.jars",
        "/path/to/ecopass_2.12-0.2.0.jar"
    )
    .getOrCreate()
)
```

### 2. Generate the index

The current EcoPass facade provides a convenience method using 2-byte headers and 2-byte alignment:

```python
from pathlib import Path

INPUT_FILE = Path("example/sample.dat")
INDEX_FILE = Path("example/sample.epidx")

eco_pass = spark._jvm.io.github.yassinessadi.ecopass.EcoPass

index = eco_pass.generateIndex(
    str(INPUT_FILE.resolve()),
    str(INDEX_FILE.resolve())
)

print("Records:", index.totalRecords())
print("Partitions:", index.partitions().size())
```

For a different fixed header size or alignment:

```python
index = eco_pass.generateIndex(
    str(INPUT_FILE.resolve()),
    str(INDEX_FILE.resolve()),
    2,
    2
)
```

### 3. Read the file as a Spark DataFrame

```python
df = (
    spark.read
    .format("ecopass")
    .option("indexPath", str(INDEX_FILE.resolve()))
    .load(str(INPUT_FILE.resolve()))
)

df.show(10, truncate=False)
```

EcoPass reads the indexed record partitions through its native Spark DataSource V2 implementation.

### 4. Process records with Spark

Filter records by status:

```python
from pyspark.sql.functions import col

normal_records = df.filter(
    col("status") == 4
)
```

Inspect record metadata:

```python
df.select(
    "status",
    "record_type",
    "length",
    "record_offset"
).show(10, truncate=False)
```

Decode payloads for text inspection:

```python
from pyspark.sql.functions import col, decode

df_text = df.withColumn(
    "data",
    decode(col("data"), "ISO-8859-1")
)

df_text.show(10, truncate=False)
```

The default `data` column remains binary to preserve the original bytes. Choose an encoding appropriate to the source file; mainframe data may use EBCDIC rather than ISO-8859-1.

## Output schema

| Column | Spark type | Description |
|---|---|---|
| `status` | IntegerType | Numeric record status |
| `record_type` | StringType | Record type name |
| `length` | LongType | Payload length |
| `data` | BinaryType | Original binary payload |
| `record_offset` | LongType | Record starting offset |
| `header_size` | IntegerType | Header size |
| `padding_size` | IntegerType | Padding size |

## Index reuse

EcoPass separates file framing rules from file-specific index data.

For example:

```text
incoming/
├── spdd.00
├── spdd.00
└── spd.a00
```

- The first `spdd.00` arrival requires index generation.
- An identical, compatible `spdd.00` file can reuse its existing index.
- A new `spd.a00` format requires its own framing configuration and index.
- A different file with the same format but different record boundaries requires a new index.

The current implementation validates indexes using file size and modification time. These checks do not guarantee byte-for-byte identity.

The current index store uses local Java NIO paths. Hadoop FileSystem and HDFS sidecar support are future improvements.

## Important behavior

- Header size must be 2 or 4 bytes.
- Status is extracted from the high nibble of the first byte.
- Payload length is decoded from the remaining 12 or 28 bits.
- Padding aligns `(headerSize + payloadLength)` to `alignment`.
- The allowed statuses mirror the Python list. Status 13 is defined but excluded,
  because the supplied Python implementation did not include it in `allowed_statuses`.
- `readRecords` returns a Java list for easy Py4J access, so it loads the file's
  records into JVM memory. It is suitable for initial testing, not huge production files.
