
package io.github.yassinessadi.ecopass.indexing

import io.github.yassinessadi.ecopass.core.RecordHeaderParser
import io.github.yassinessadi.ecopass.framing._

import java.io.{BufferedInputStream, EOFException}
import java.nio.file.{Files, Paths}
import scala.collection.Iterator

object EcoPassIndexGenerator {

  /**
   * Generate an index using the supplied framing policy.
   *
   * Payloads are skipped instead of loaded into memory.
   */
  def generate(
      sourcePath: String,
      indexPath: String,
      policy: RecordFramingPolicy,
      targetPartitionBytes: Long =
        EcoPassIndexBuilder.DefaultTargetPartitionBytes
  ): EcoPassIndex = {

    require(sourcePath != null && sourcePath.nonEmpty,
      "sourcePath cannot be empty")

    require(indexPath != null && indexPath.nonEmpty,
      "indexPath cannot be empty")

    require(policy != null, "Framing policy cannot be null")

    val source = Paths.get(sourcePath).toAbsolutePath.normalize()
    val destination = Paths.get(indexPath).toAbsolutePath.normalize()

    require(Files.exists(source), s"Source file does not exist: $source")
    require(Files.isRegularFile(source), s"Source is not a regular file: $source")

    val fileSize = Files.size(source)
    val modificationTime = Files.getLastModifiedTime(source).toMillis

    val input = new BufferedInputStream(Files.newInputStream(source))

    var currentOffset = 0L

    def readFully(buffer: Array[Byte], offset: Int, length: Int): Unit = {
      var total = 0

      while (total < length) {
        val count = input.read(buffer, offset + total, length - total)

        if (count < 0) {
          throw new EOFException(
            s"Unexpected EOF while reading header at offset $currentOffset"
          )
        }

        if (count == 0) {
          val value = input.read()

          if (value < 0) {
            throw new EOFException(
              s"Unexpected EOF while reading header at offset $currentOffset"
            )
          }

          buffer(offset + total) = value.toByte
          total += 1
        } else {
          total += count
        }
      }
    }

    def skipFully(length: Long): Unit = {
      var remaining = length

      while (remaining > 0L) {
        val skipped = input.skip(remaining)

        if (skipped > 0L) {
          remaining -= skipped
        } else {
          if (input.read() < 0) {
            throw new EOFException(
              s"Unexpected EOF while skipping record at offset $currentOffset"
            )
          }

          remaining -= 1L
        }
      }
    }

    def readBoundary(): Option[RecordBoundary] = {

      val firstByte = input.read()

      if (firstByte < 0) {
        None
      } else {

        val recordOffset = currentOffset

        val headerSize = policy.resolveHeaderSize(
          recordOffset,
          firstByte
        )

        val headerBytes = new Array[Byte](headerSize)
        headerBytes(0) = firstByte.toByte

        readFully(
          headerBytes,
          1,
          headerSize - 1
        )

        val header = RecordHeaderParser.parse(
          headerBytes,
          headerSize
        )

        policy.validatePayloadLength(header.length)

        val paddingSize = policy.resolvePadding(
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

        RecordBoundaryCalculator.validateAgainstFileSize(
          boundary,
          fileSize
        )

        // Skip payload and padding without allocating a payload array.
        skipFully(header.length)
        skipFully(paddingSize.toLong)

        currentOffset = boundary.endOffset

        Some(boundary)
      }
    }

    try {

      val boundaries: Iterator[RecordBoundary] =
        Iterator.continually(readBoundary())
          .takeWhile(_.nonEmpty)
          .map(_.get)

      val index = EcoPassIndexBuilder.build(
        sourcePath = source.toString,
        fileSize = fileSize,
        modificationTime = modificationTime,
        boundaries = boundaries,
        targetPartitionBytes = targetPartitionBytes
      )

      // Detect changes to the source while the index was being generated.
      require(
        Files.size(source) == fileSize &&
        Files.getLastModifiedTime(source).toMillis == modificationTime,
        "Source file changed during index generation"
      )

      EcoPassIndexStore.write(index, destination)

      index

    } finally {
      input.close()
    }
  }

  /**
   * Convenience method for the default 2-byte / 2-byte-alignment format.
   */
  def generate(
      sourcePath: String,
      indexPath: String,
      headerSize: Int,
      alignment: Int
  ): EcoPassIndex = {

    val policy = RecordFramingPolicy(
      headerFormatResolver = HeaderFormatResolver.Fixed(headerSize),
      paddingResolver = PaddingResolver.FixedAlignment(alignment),
      invalidStatusPolicy = InvalidStatusPolicy.Keep
    )

    generate(sourcePath, indexPath, policy)
  }
}
