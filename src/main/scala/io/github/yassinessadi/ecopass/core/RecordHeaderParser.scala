package io.github.yassinessadi.ecopass.core

import io.github.yassinessadi.ecopass.RecordType

import java.io.{EOFException, InputStream}

final case class ParsedRecordHeader(
    status: Int,
    length: Long,
    headerSize: Int
) extends Serializable {

  def recordTypeName: String =
    RecordType.nameOf(status)

  def isKnownStatus: Boolean =
    !recordTypeName.startsWith("UNKNOWN_")
}

object RecordHeaderParser {

  val MinHeaderSize: Int = 2
  val MaxHeaderSize: Int = 4

  val MaxTwoByteLength: Long = 4095L
  val MaxFourByteLength: Long = 268435455L

  def parse(
      headerBytes: Array[Byte],
      headerSize: Int
  ): ParsedRecordHeader = {

    require(headerBytes != null, "Header cannot be null")

    require(
      headerSize == 2 || headerSize == 4,
      s"Unsupported header size: $headerSize"
    )

    require(
      headerBytes.length >= headerSize,
      s"Expected $headerSize bytes, received ${headerBytes.length}"
    )

    val byte0 = headerBytes(0) & 0xFF

    val status = (byte0 >>> 4) & 0x0F

    val length: Long = headerSize match {

      case 2 =>
        val byte1 = headerBytes(1) & 0xFF

        ((byte0 & 0x0F).toLong << 8) |
          byte1.toLong

      case 4 =>
        val byte1 = headerBytes(1) & 0xFF
        val byte2 = headerBytes(2) & 0xFF
        val byte3 = headerBytes(3) & 0xFF

        ((byte0 & 0x0F).toLong << 24) |
          (byte1.toLong << 16) |
          (byte2.toLong << 8) |
          byte3.toLong
    }

    ParsedRecordHeader(
      status = status,
      length = length,
      headerSize = headerSize
    )
  }

  def read(
      input: InputStream,
      headerSize: Int
  ): Option[ParsedRecordHeader] = {

    require(input != null, "Input stream cannot be null")

    require(
      headerSize == 2 || headerSize == 4,
      s"Unsupported header size: $headerSize"
    )

    val bytes = new Array[Byte](headerSize)

    val first = input.read()

    if (first < 0) {
      return None
    }

    bytes(0) = first.toByte

    readFully(
      input,
      bytes,
      offset = 1,
      length = headerSize - 1
    )

    Some(parse(bytes, headerSize))
  }

  private def readFully(
      input: InputStream,
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
          "Unexpected EOF while reading record header"
        )
      }

      if (count == 0) {
        val value = input.read()

        if (value < 0) {
          throw new EOFException(
            "Unexpected EOF while reading record header"
          )
        }

        buffer(offset + totalRead) = value.toByte
        totalRead += 1
      } else {
        totalRead += count
      }
    }
  }
}