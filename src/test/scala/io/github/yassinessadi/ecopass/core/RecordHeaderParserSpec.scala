
package io.github.yassinessadi.ecopass.core

import java.io.ByteArrayInputStream
import org.scalatest.funsuite.AnyFunSuite

class RecordHeaderParserSpec extends AnyFunSuite {

  test("should parse a 2-byte header") {
    // Status = 4, length = 5
    val header = Array[Byte](0x40, 0x05)

    val parsed = RecordHeaderParser.parse(header, 2)

    assert(parsed.status == 4)
    assert(parsed.length == 5L)
    assert(parsed.headerSize == 2)
  }

  test("should parse a 4-byte header") {
    // Status = 4, length = 5
    val header = Array[Byte](0x40, 0x00, 0x00, 0x05)

    val parsed = RecordHeaderParser.parse(header, 4)

    assert(parsed.status == 4)
    assert(parsed.length == 5L)
    assert(parsed.headerSize == 4)
  }

  test("should parse the maximum 2-byte length") {
    // val header = Array[Byte](0x4F, 0xFF)
    val header = Array[Byte](0x4F.toByte, 0xFF.toByte)

    val parsed = RecordHeaderParser.parse(header, 2)

    assert(parsed.status == 4)
    assert(parsed.length == 4095L)
  }

  test("should parse an unsigned byte value") {
    val header = Array[Byte](0x4F.toByte, 0xFF.toByte)

    val parsed = RecordHeaderParser.parse(header, 2)

    assert(parsed.status == 4)
    assert(parsed.length == 4095L)
  }

  test("should reject unsupported header sizes") {
    intercept[IllegalArgumentException] {
      RecordHeaderParser.parse(
        Array[Byte](0x40, 0x05, 0x00),
        3
      )
    }
  }

  test("should reject a header buffer with insufficient bytes") {
    intercept[IllegalArgumentException] {
      RecordHeaderParser.parse(
        Array[Byte](0x40),
        2
      )
    }
  }

  test("should read a header from an input stream") {
    val input = new ByteArrayInputStream(
      Array[Byte](0x40, 0x05)
    )

    val parsed = RecordHeaderParser.read(input, 2)

    assert(parsed.isDefined)
    assert(parsed.get.status == 4)
    assert(parsed.get.length == 5L)
  }

  test("should return None when the stream is empty") {
    val input = new ByteArrayInputStream(Array.emptyByteArray)

    assert(RecordHeaderParser.read(input, 2).isEmpty)
  }
}
