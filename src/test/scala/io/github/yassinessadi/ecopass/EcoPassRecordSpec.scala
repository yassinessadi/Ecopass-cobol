package io.github.yassinessadi.ecopass

import org.scalatest.funsuite.AnyFunSuite

class EcoPassRecordSpec extends AnyFunSuite {

  test("should create an EcoPass record") {
    val payload = "HELLO".getBytes("UTF-8")

    val record = EcoPassRecord(
      status = 4,
      length = payload.length,
      data = payload,
      offset = 0L
    )

    assert(record.status == 4)
    assert(record.length == 5)
    assert(record.recordTypeName == "NORMAL_USER_DATA")
    assert(new String(record.data, "UTF-8") == "HELLO")
  }
}