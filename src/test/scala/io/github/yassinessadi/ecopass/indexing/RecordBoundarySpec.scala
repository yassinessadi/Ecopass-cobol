
package io.github.yassinessadi.ecopass.indexing

import io.github.yassinessadi.ecopass.framing.RecordBoundaryCalculator
import org.scalatest.funsuite.AnyFunSuite

class RecordBoundarySpec extends AnyFunSuite {

  test("should calculate a record boundary") {
    val boundary = RecordBoundaryCalculator.calculate(
      0L, 4, 2, 5L, 1
    )

    assert(boundary.startOffset == 0L)
    assert(boundary.endOffset == 8L)
    assert(boundary.status == 4)
    assert(boundary.headerSize == 2)
    assert(boundary.payloadLength == 5L)
    assert(boundary.paddingSize == 1)
  }

  test("should calculate boundaries for records after the first") {
    val boundary = RecordBoundaryCalculator.calculate(
      100L, 4, 4, 10L, 2
    )

    assert(boundary.startOffset == 100L)
    assert(boundary.endOffset == 116L)
  }

  test("should validate a boundary against file size") {
    val boundary = RecordBoundaryCalculator.calculate(
      0L, 4, 2, 5L, 1
    )

    // Valid boundary: should complete without throwing.
    RecordBoundaryCalculator.validateAgainstFileSize(
      boundary,
      8L
    )
  }

  test("should reject a boundary exceeding file size") {
    val boundary = RecordBoundaryCalculator.calculate(
      0L, 4, 2, 5L, 1
    )

    intercept[IllegalArgumentException] {
      RecordBoundaryCalculator.validateAgainstFileSize(
        boundary,
        7L
      )
    }
  }

  test("should detect offset overflow") {
    intercept[ArithmeticException] {
      RecordBoundaryCalculator.calculate(
        Long.MaxValue - 2L,
        4,
        4,
        10L,
        0
      )
    }
  }
}
