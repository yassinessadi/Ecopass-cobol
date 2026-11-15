
package io.github.yassinessadi.ecopass.core

import io.github.yassinessadi.ecopass.framing.{
  RecordBoundaryCalculator,
  RecordFramingPolicy,
  PaddingContext
}

import java.io.{EOFException, InputStream}

final class EcoPassRecordReader(
    input: InputStream,
    framingPolicy: RecordFramingPolicy,
    initialOffset: Long = 0L
) extends AutoCloseable {

  require(input != null, "Input stream cannot be null")
  require(framingPolicy != null, "Framing policy cannot be null")
  require(initialOffset >= 0L, "Initial offset cannot be negative")

  private var currentOffset: Long = initialOffset
  private var closed: Boolean = false

  def offset: Long = currentOffset

  def readNext(): Option[EcoPassRecord] = {
    ensureOpen()

    var result: Option[EcoPassRecord] = None
    var found = false

    while (!found) {
      val firstByte = input.read()

      if (firstByte < 0) {
        found = true
      } else {
        val recordOffset = currentOffset

        val headerSize = framingPolicy.resolveHeaderSize(
          recordOffset,
          firstByte
        )

        val headerBytes = new Array[Byte](headerSize)
        headerBytes(0) = firstByte.toByte

        readFully(
          headerBytes,
          offset = 1,
          length = headerSize - 1
        )

        val header = RecordHeaderParser.parse(
          headerBytes,
          headerSize
        )

        framingPolicy.validatePayloadLength(header.length)

        val paddingSize = framingPolicy.resolvePadding(
          PaddingContext(
            recordOffset,
            header.status,
            headerSize,
            header.length
          )
        )

        val boundary = RecordBoundaryCalculator.calculate(
          recordOffset,
          header.status,
          headerSize,
          header.length,
          paddingSize
        )

        val payload = readPayload(header.length)

        skipFully(paddingSize)

        currentOffset = boundary.endOffset

        if (framingPolicy.shouldEmit(header.status)) {
          result = Some(
            EcoPassRecord(
              status = header.status,
              length = header.length,
              data = payload,
              offset = recordOffset,
              headerSize = headerSize,
              paddingSize = paddingSize
            )
          )
          found = true
        }
      }
    }

    result
  }

  private def readPayload(length: Long): Array[Byte] = {
    if (length > Int.MaxValue) {
      throw new IllegalArgumentException(
        s"Payload is too large for a JVM byte array: $length"
      )
    }

    val result = new Array[Byte](length.toInt)

    readFully(
      result,
      offset = 0,
      length = result.length
    )

    result
  }

  private def readFully(
      buffer: Array[Byte],
      offset: Int,
      length: Int
  ): Unit = {
    var totalRead = 0

    while (totalRead < length) {
      val count = input.read(
        buffer,
        offset + totalRead,
        length - totalRead
      )

      if (count < 0) {
        throw new EOFException(
          s"Unexpected EOF at offset $currentOffset"
        )
      }

      if (count == 0) {
        val value = input.read()

        if (value < 0) {
          throw new EOFException(
            s"Unexpected EOF at offset $currentOffset"
          )
        }

        buffer(offset + totalRead) = value.toByte
        totalRead += 1
      } else {
        totalRead += count
      }
    }
  }

  private def skipFully(length: Int): Unit = {
    var remaining = length

    while (remaining > 0) {
      val skipped = input.skip(remaining.toLong)

      if (skipped > 0) {
        remaining -= skipped.toInt
      } else {
        if (input.read() < 0) {
          throw new EOFException(
            s"Unexpected EOF while skipping padding at offset $currentOffset"
          )
        }

        remaining -= 1
      }
    }
  }

  private def ensureOpen(): Unit = {
    if (closed) {
      throw new IllegalStateException(
        "EcoPassRecordReader is closed"
      )
    }
  }

  override def close(): Unit = {
    if (!closed) {
      closed = true
      input.close()
    }
  }
}
