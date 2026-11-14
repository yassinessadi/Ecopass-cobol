package io.github.yassinessadi.ecopass

import java.io.RandomAccessFile
import java.util.{ArrayList, List => JList}

/**
 * Reads a file containing sequential 2-byte or 4-byte record headers.
 *
 * Header layout:
 *   - high 4 bits of byte 0: record status
 *   - remaining 12 bits (2-byte header) or 28 bits (4-byte header): payload length
 *
 * The payload is followed by padding so (header size + payload length) is aligned
 * to the configured alignment.
 */
final class RecordHeaderReader(
  val headerSize: Int = 2,
  val alignment: Int = 2,
  val debug: Boolean = false
) extends Serializable {

  require(headerSize == 2 || headerSize == 4,
    "Only 2-byte or 4-byte headers are supported")
  require(alignment > 0, "alignment must be greater than zero")

  // Mirrors the statuses allowed by the supplied Python implementation.
  // Status 13 is defined in RecordType but was not included in its allowed list.
  private val allowedStatuses: Set[Int] = Set(
    RecordType.NULL_RECORD,
    RecordType.NORMAL_USER_DATA,
    RecordType.DELETED_RECORD,
    RecordType.SYSTEM_RESERVED,
    RecordType.USER_DATA_RECORD,
    RecordType.RESERVED_FOR_FUTURE_USE,
    RecordType.SYSTEM_RECORD_DUPLICATE,
    RecordType.POINTER_RECORD,
    RecordType.REDUCED_USER_DATA,
    RecordType.MID_TRANSACTION_USER_RECORD,
    RecordType.MID_TRANSACTION_REDUCED_USER_RECORD,
    RecordType.MID_TRANSACTION_USER_RECORD_REFERENCED
  )

  private def unsigned(b: Byte): Int = b & 0xff

  private def parseHeader(bytes: Array[Byte]): (Int, Int) = {
    val b0 = unsigned(bytes(0))
    val status = (b0 >>> 4) & 0x0f

    val length =
      if (headerSize == 2) {
        ((b0 & 0x0f) << 8) | unsigned(bytes(1))
      } else {
        ((b0 & 0x0f) << 24) |
          (unsigned(bytes(1)) << 16) |
          (unsigned(bytes(2)) << 8) |
          unsigned(bytes(3))
      }

    (status, length)
  }

  private def paddingSize(length: Int): Int = {
    val total = headerSize.toLong + length.toLong
    ((alignment - (total % alignment)) % alignment).toInt
  }

  /**
   * Reads all accepted records into a Java List, making this method convenient
   * to call through Py4J from PySpark.
   *
   * Note: this method materializes the records in driver/JVM memory. For very
   * large files, use a distributed Hadoop InputFormat or a streaming partition reader.
   */
  def readRecords(filename: String): JList[EcoPassRecord] = {
    val result = new ArrayList[EcoPassRecord]()
    val raf = new RandomAccessFile(filename, "r")

    try {
      val fileLength = raf.length()

      while (raf.getFilePointer < fileLength) {
        val recordOffset = raf.getFilePointer
        val remaining = fileLength - recordOffset

        if (remaining < headerSize) {
          if (debug) {
            System.err.println(
              s"[EcoPass] Incomplete header at $recordOffset: " +
                s"$remaining byte(s), expected $headerSize"
            )
          }
          // Same end-of-file behavior as the Python implementation.
          raf.seek(fileLength)
        } else {
          val header = new Array[Byte](headerSize)
          raf.readFully(header)

          val (status, length) = parseHeader(header)

          if (length < 0 || length.toLong > fileLength - raf.getFilePointer) {
            throw new IllegalArgumentException(
              s"Incomplete record at position $recordOffset: expected $length payload bytes"
            )
          }

          val payload = new Array[Byte](length)
          raf.readFully(payload)

          if (allowedStatuses.contains(status)) {
            result.add(EcoPassRecord(status, length, payload, recordOffset))
          } else if (debug) {
            System.err.println(s"[EcoPass] Skipping unsupported status $status at $recordOffset")
          }

          val padding = paddingSize(length)
          if (padding.toLong > fileLength - raf.getFilePointer) {
            throw new IllegalArgumentException(
              s"Incomplete padding at position ${raf.getFilePointer}: expected $padding byte(s)"
            )
          }
          raf.seek(raf.getFilePointer + padding)
        }
      }

      result
    } catch {
      case e: Exception =>
        throw new IllegalArgumentException(
          s"Failed to read EcoPass records from '$filename': ${e.getMessage}", e
        )
    } finally {
      raf.close()
    }
  }
}
