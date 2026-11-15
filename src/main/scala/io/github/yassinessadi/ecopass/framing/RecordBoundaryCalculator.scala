
package io.github.yassinessadi.ecopass.framing

import io.github.yassinessadi.ecopass.indexing.RecordBoundary

object RecordBoundaryCalculator {

  /**
   * Calculate a record's physical boundary.
   *
   * endOffset is exclusive and includes padding.
   */
  def calculate(
      recordOffset: Long,
      status: Int,
      headerSize: Int,
      payloadLength: Long,
      paddingSize: Int
  ): RecordBoundary = {

    require(recordOffset >= 0L, "recordOffset cannot be negative")
    require(status >= 0 && status <= 15, "Invalid record status")

    require(
      headerSize == 2 || headerSize == 4,
      "Header size must be 2 or 4"
    )

    require(payloadLength >= 0L, "Payload length cannot be negative")
    require(paddingSize >= 0, "Padding cannot be negative")

    val payloadOffset = Math.addExact(
      recordOffset,
      headerSize.toLong
    )

    val payloadEndOffset = Math.addExact(
      payloadOffset,
      payloadLength
    )

    val endOffset = Math.addExact(
      payloadEndOffset,
      paddingSize.toLong
    )

    RecordBoundary(
      startOffset = recordOffset,
      endOffset = endOffset,
      status = status,
      headerSize = headerSize,
      payloadLength = payloadLength,
      paddingSize = paddingSize
    )
  }

  /**
   * Ensure the calculated record does not exceed file size.
   */
  def validateAgainstFileSize(
      boundary: RecordBoundary,
      fileSize: Long
  ): Unit = {

    require(fileSize >= 0L, "fileSize cannot be negative")

    require(
      boundary.endOffset <= fileSize,
      s"Record at ${boundary.startOffset} exceeds file size $fileSize"
    )
  }

  /**
   * Calculate the next record offset.
   */
  def nextOffset(boundary: RecordBoundary): Long = {
    boundary.endOffset
  }
}
