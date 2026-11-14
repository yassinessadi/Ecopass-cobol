package io.github.yassinessadi.ecopass

import org.scalatest.funsuite.AnyFunSuite

class RecordTypeSpec extends AnyFunSuite {

  test("should resolve known record types") {
    assert(RecordType.nameOf(4) == "NORMAL_USER_DATA")
    assert(RecordType.nameOf(3) == "SYSTEM_RESERVED")
    assert(RecordType.nameOf(7) == "USER_DATA_RECORD")
  }
}