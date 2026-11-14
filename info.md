# EcoPass Record Header Reader

## 1. Overview

The EcoPass Record Header Reader is a Scala module designed to read files containing sequential records with 2-byte or 4-byte headers.

It extracts:

- Record status
- Record length
- Record payload
- Record offset within the source file

The implementation uses Java's `RandomAccessFile` to read file contents sequentially.

## 2. Record Structure

Each record consists of three components:

```text
+------------------+----------------------+------------------+
| Header           | Record Payload       | Padding          |
+------------------+----------------------+------------------+
| 2 or 4 bytes     | Variable length      | 0 or more bytes  |
+------------------+----------------------+------------------+
```

### 2.1 Header structure

The header supports two formats:

- 2-byte header
- 4-byte header

The first 4 bits of the first byte represent the record status. The remaining bits represent the payload length.

#### 2-byte header

```text
Byte 0                  Byte 1
+----------+------------+----------------+
| 4 bits   | 4 bits     | 8 bits         |
+----------+------------+----------------+
| Status   | Length     | Length         |
+----------+------------+----------------+
```

The payload length is encoded using 12 bits.

Calculation:

```text
Maximum length = 2^12 - 1
               = 4,095 bytes
```

#### 4-byte header

```text
Byte 0                  Byte 1
+----------+------------+----------------+
| 4 bits   | 4 bits     | 8 bits         |
+----------+------------+----------------+
| Status   | Length     | Length         |
+----------+------------+----------------+

Byte 2                  Byte 3
+-----------------------+----------------+
| 8 bits                | 8 bits         |
+-----------------------+----------------+
| Length                | Length         |
+-----------------------+----------------+
```

The payload length is encoded using 28 bits.

Calculation:

```text
Maximum length = 2^28 - 1
               = 268,435,455 bytes
               ≈ 256 MiB
```

This is the maximum payload size representable by the 4-byte header format.

## 3. Record Status

The record status is extracted from the first 4 bits of the first header byte.

```scala
val status = (b0 >>> 4) & 0x0f
```

Supported status definitions:

| Value | Record Type |
|---:|---|
| 0 | NULL_RECORD |
| 1 | SYSTEM_RECORD_DUPLICATE |
| 2 | DELETED_RECORD |
| 3 | SYSTEM_RESERVED |
| 4 | NORMAL_USER_DATA |
| 5 | REDUCED_USER_DATA |
| 6 | POINTER_RECORD |
| 7 | USER_DATA_RECORD |
| 8 | REDUCED_USER_DATA_REFERENCED_BY_POINTER |
| 9 | RESERVED_FOR_FUTURE_USE |
| 10 | MID_TRANSACTION_USER_RECORD |
| 11 | MID_TRANSACTION_REDUCED_USER_RECORD |
| 12 | MID_TRANSACTION_USER_RECORD_REFERENCED |
| 13 | MID_TRANSACTION_REDUCED_USER_RECORD_REFERENCED |

Only statuses included in `RecordHeaderReader.allowedStatuses` are returned.

#### Status 0 – Null Record

Status `0` (`0000` in binary) is not listed as a standard record type in the Micro Focus file-header documentation.

However, an independent Micro Focus COBOL file parser, `mfcobol-export`, identifies status `0` as `TYPE_NULL` and handles it as a null record.

##### Example

Consider the following hypothetical binary record header:

```text
Byte 0       Byte 1
+----------+----------+
| 0000     | 00000000 |
+----------+----------+
```

Binary interpretation:

```text
Status = 0000
Length = 000000000000
```

Decoded values:

```text
Status: 0
Type: NULL_RECORD
Length: 0
Payload: Empty
```

##### EcoPass interpretation

EcoPass can recognize this status explicitly:

```scala
object RecordType {
  val NULL_RECORD = 0
  // Other record types...
}
```

The reader should handle it separately from normal user records:

```scala
status match {
  case RecordType.NULL_RECORD =>
    // Handle or skip the null record

  case s if allowedStatuses.contains(s) =>
    // Process the normal record

  case _ =>
    // Handle unsupported status
}
```

##### Important considerations

- Status `0` is not formally defined in the cited Micro Focus record-type table.
- Its interpretation as a null record comes from an independent parser implementation.
- The example above is illustrative, not a verified binary sample from a real Micro Focus file.
- Do not assume every status-0 header has a zero-length payload without validating actual files.

**EcoPass implementation decision:** recognize status `0` as `NULL_RECORD`, keep it separate from normal user records, and make its handling configurable until verified against real source files.

## 4. Record Alignment and Padding

Records can be aligned to a configurable boundary.

The padding calculation is:

```text
padding = (alignment - ((headerSize + length) % alignment))
          % alignment
```

Where:

- `padding` = number of padding bytes
- `alignment` = configured alignment
- `headerSize` = header size in bytes
- `length` = payload length in bytes

### Example

Consider:

```text
Header size = 2 bytes
Payload length = 5 bytes
Alignment = 2 bytes
```

Calculation:

```text
Total size = header size + payload length
           = 2 + 5
           = 7 bytes

Padding = (2 - (7 % 2)) % 2
        = 1 byte

Final record size = 2 + 5 + 1
                  = 8 bytes
```

The next record starts after the padding.

## 5. File Reading Process

The reader processes the source file sequentially:

1. Open the file using `RandomAccessFile`.
2. Read the configured header size.
3. Extract the status and payload length.
4. Validate that the payload fits within the remaining file.
5. Read the payload into a byte array.
6. Check whether the record status is supported.
7. Add accepted records to the result collection.
8. Calculate and skip alignment padding.
9. Continue until the end of the file.

The file is closed after reading, including when an exception occurs.

### Main Scala classes

| Class | Responsibility |
|---|---|
| `RecordHeaderReader` | Reads and parses records |
| `RecordType` | Defines record status values |
| `EcoPassRecord` | Represents an individual record |

## 6. Data Types and Memory Limitations

The implementation uses different numeric types depending on the operation.

| Data | Scala / Java type | Reason |
|---|---|---|
| File length | `Long` | Supports large file offsets |
| File position | `Long` | Supports positions beyond 2 GB |
| Record offset | `Long` | Preserves the original file position |
| Record length | `Int` | Header length is at most 28 bits |
| Record payload | `Array[Byte]` | JVM byte-array representation |
| Record collection | `ArrayList` | Stores all accepted records |

### 6.1 Java Integer limitation

Java's `int` is a signed 32-bit integer.

Its range is:

```text
Minimum = -2^31
        = -2,147,483,648

Maximum = 2^31 - 1
        = 2,147,483,647
        ≈ 2 GiB
```

Java arrays use integer-based indexing and cannot represent arbitrarily large individual arrays.

In practice, the maximum usable array size is slightly smaller than `Integer.MAX_VALUE` and depends on JVM implementation and available heap memory.

### 6.2 File-size limitation

The current implementation uses:

```scala
val fileLength = raf.length()
```

and:

```scala
val recordOffset = raf.getFilePointer
```

Both values are `Long`.

Therefore, `RandomAccessFile` can address files larger than 2 GB.

However, the current `readRecords()` method stores all accepted records in memory:

```scala
val result = new ArrayList[EcoPassRecord]()
```

Each payload is also stored as an individual byte array:

```scala
val payload = new Array[Byte](length)
```

This creates memory limitations.

The actual limits depend on:

- Available JVM heap memory.
- Maximum individual byte-array size.
- Number of records.
- Memory consumed by record objects and collection references.

**Important:** A file larger than 2 GB is not automatically unsupported. The current implementation can address large files, but it is not suitable for arbitrarily large files because it loads all accepted records into memory.

### 6.3 Current implementation capabilities

| Capability | Current implementation |
|---|---|
| Files larger than 2 GB | Addressable, subject to available memory |
| Files larger than 10 GB | Not recommended with the current implementation |
| Individual payload larger than approximately 256 MiB | Not supported by the 4-byte header format |
| File offsets larger than 2 GB | Supported through `Long` |
| Streaming records | Not implemented |
| Distributed file processing | Not implemented |

## 7. Future Scalability: Streaming Reader

To process large files without loading all records into memory, the reader should be redesigned around a streaming API.

For example:

```scala
def readRecords(filename: String): Iterator[EcoPassRecord]
```

A production implementation should also manage the file resource safely, using a closeable iterator or callback-based reader.

The processing model would become:

```text
Source File
    |
    v
Read Header
    |
    v
Extract Status and Length
    |
    v
Read One Payload
    |
    v
Process Record
    |
    v
Release Payload
    |
    v
Skip Padding
    |
    v
Read Next Header
```

This avoids retaining the entire file in memory.

For distributed Spark processing, a further step would be to implement a Hadoop-compatible input reader that understands record boundaries and safely assigns records to partitions.

A normal byte-range split is not sufficient because a split could start in the middle of a record.

## 8. Example

Given three records:

```text
Record 1:
Status  = 4
Length  = 5
Payload = HELLO

Record 2:
Status  = 3
Length  = 7
Payload = ECOPASS

Record 3:
Status  = 7
Length  = 5
Payload = SPARK
```

Expected output:

```text
Record 1: status=4, type=NORMAL_USER_DATA, length=5, data=HELLO
Record 2: status=3, type=SYSTEM_RESERVED, length=7, data=ECOPASS
Record 3: status=7, type=USER_DATA_RECORD, length=5, data=SPARK
```

## 9. Summary

The EcoPass Record Header Reader provides a Scala implementation for parsing sequential records with 2-byte and 4-byte headers.

Its current implementation supports:

- Sequential record reading.
- Status extraction.
- Payload length extraction.
- Configurable alignment and padding.
- File offsets represented by `Long`.
- Integration with PySpark through the JVM.

The current implementation is intended for testing and files that can reasonably fit into available JVM memory.

For production processing of very large files, a streaming implementation should be preferred. For distributed processing, a split-aware Hadoop input implementation should be considered.

The 4-byte header format limits each individual record payload to 268,435,455 bytes (approximately 256 MiB), regardless of the total source file size.
