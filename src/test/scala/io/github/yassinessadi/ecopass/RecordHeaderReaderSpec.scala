package io.github.yassinessadi.ecopass

import org.scalatest.funsuite.AnyFunSuite

class RecordHeaderReaderSpec extends AnyFunSuite {

  test("should initialize with default configuration") {
    val reader = new RecordHeaderReader()

    assert(reader.headerSize == 2)
    assert(reader.alignment == 2)
  }

  test("should reject an unsupported header size") {
    assertThrows[IllegalArgumentException] {
      new RecordHeaderReader(headerSize = 3)
    }
  }

  test("should reject an invalid alignment") {
    assertThrows[IllegalArgumentException] {
      new RecordHeaderReader(alignment = 0)
    }
  }
}