
package io.github.yassinessadi.ecopass.core

import org.scalatest.funsuite.AnyFunSuite

class EcoPassRecordSpec extends AnyFunSuite {

  test("should create a record with its payload") {
    val payload = "HELLO".getBytes("UTF-8")

    val record = EcoPassRecord(
      status = 4,
      length = payload.length,
      data = payload,
      offset = 0L
    )

    assert(record.status == 4)
    assert(record.length == 5L)
    assert(new String(record.data, "UTF-8") == "HELLO")
    assert(record.offset == 0L)
  }

  test("should preserve header and padding metadata") {
    val record = EcoPassRecord(
      status = 4,
      length = 3L,
      data = Array[Byte](65, 66, 67),
      offset = 10L,
      headerSize = 4,
      paddingSize = 1
    )

    assert(record.headerSize == 4)
    assert(record.paddingSize == 1)
    assert(record.offset == 10L)
  }

  test("should reject a negative length") {
    intercept[IllegalArgumentException] {
      EcoPassRecord(
        status = 4,
        length = -1L,
        data = Array.emptyByteArray
      )
    }
  }

  test("should reject a payload length mismatch") {
    intercept[IllegalArgumentException] {
      EcoPassRecord(
        status = 4,
        length = 10L,
        data = Array[Byte](1, 2, 3)
      )
    }
  }

  test("should reject an invalid status") {
    intercept[IllegalArgumentException] {
      EcoPassRecord(
        status = 16,
        length = 0L,
        data = Array.emptyByteArray
      )
    }
  }

  test("should support empty records") {
    val record = EcoPassRecord(
      status = 0,
      length = 0L,
      data = Array.emptyByteArray
    )

    assert(record.length == 0L)
    assert(record.data.isEmpty)
  }
}
