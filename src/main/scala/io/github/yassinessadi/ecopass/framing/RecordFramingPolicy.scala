
package io.github.yassinessadi.ecopass.framing

import io.github.yassinessadi.ecopass.RecordType

sealed trait InvalidStatusPolicy extends Serializable

object InvalidStatusPolicy {

  /**
   * Keep the record even if its status is unknown.
   */
  case object Keep extends InvalidStatusPolicy

  /**
   * Skip the record, but only if its header and payload
   * boundaries can still be determined safely.
   */
  case object Skip extends InvalidStatusPolicy

  /**
   * Stop processing when an unknown status is encountered.
   */
  case object Fail extends InvalidStatusPolicy
}

/**
 * Complete framing configuration for EcoPass.
 */
final case class RecordFramingPolicy(
    headerFormatResolver: HeaderFormatResolver,
    paddingResolver: PaddingResolver,
    invalidStatusPolicy: InvalidStatusPolicy =
      InvalidStatusPolicy.Skip,
    maxPayloadLength: Long = 268435455L
) extends Serializable {

  require(
    headerFormatResolver != null,
    "headerFormatResolver cannot be null"
  )

  require(
    paddingResolver != null,
    "paddingResolver cannot be null"
  )

  require(
    invalidStatusPolicy != null,
    "invalidStatusPolicy cannot be null"
  )

  require(
    maxPayloadLength >= 0L &&
    maxPayloadLength <= 268435455L,
    "maxPayloadLength must be between 0 and 268435455"
  )

  /**
   * Resolve header size from the first header byte.
   */
  def resolveHeaderSize(
      recordOffset: Long,
      firstByte: Int
  ): Int = {

    val size = headerFormatResolver.resolve(
      recordOffset,
      firstByte
    )

    require(
      size == 2 || size == 4,
      s"Resolver returned invalid header size: $size"
    )

    size
  }

  /**
   * Resolve padding for a parsed record.
   */
  def resolvePadding(
      context: PaddingContext
  ): Int = {

    val padding = paddingResolver.resolve(context)

    require(
      padding >= 0,
      s"Padding cannot be negative: $padding"
    )

    padding
  }

  /**
   * Determine whether a status is known.
   *
   * Status 0 (NULL_RECORD) is valid.
   * Statuses 14 and 15 are unknown.
   */
  def isKnownStatus(status: Int): Boolean = {
    status >= RecordType.NULL_RECORD &&
    status <= RecordType.MID_TRANSACTION_REDUCED_USER_RECORD_REFERENCED
  }

  /**
   * Determine whether a parsed record should be emitted.
   */
  def shouldEmit(status: Int): Boolean = {

    if (isKnownStatus(status)) {
      true
    } else {
      invalidStatusPolicy match {
        case InvalidStatusPolicy.Keep => true
        case InvalidStatusPolicy.Skip => false
        case InvalidStatusPolicy.Fail =>
          throw new IllegalArgumentException(
            s"Unknown EcoPass record status: $status"
          )
      }
    }
  }

  /**
   * Validate a decoded payload length.
   */
  def validatePayloadLength(length: Long): Unit = {

    require(
      length >= 0L,
      s"Negative payload length: $length"
    )

    require(
      length <= maxPayloadLength,
      s"Payload length $length exceeds configured maximum $maxPayloadLength"
    )
  }
}
