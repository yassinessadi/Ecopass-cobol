
package io.github.yassinessadi.ecopass.framing

/**
 * Information required to calculate record padding.
 */

final case class PaddingContext(
    recordOffset: Long,
    status: Int,
    headerSize: Int,
    payloadLength: Long
) extends Serializable{

  require(recordOffset >= 0L, "recordOffset cannot be negative")
  require(status >= 0 && status <= 15, "Invalid status")
  require(headerSize == 2 || headerSize == 4, "Invalid header size")
  require(payloadLength >= 0L, "payloadLength cannot be negative")
}

/**
 * Resolves padding after a record payload.
 */
trait PaddingResolver extends Serializable {
  def resolve(context: PaddingContext): Int
}

object PaddingResolver {

  /**
   * Records have no padding.
   */
  case object None extends PaddingResolver {

    override def resolve(context: PaddingContext): Int = 0
  }

  /**
   * Calculate padding using fixed alignment.
   *
   * Equivalent to the Python formula:
   *
   * (alignment - ((headerSize + length) % alignment)) % alignment
   */
  final case class FixedAlignment(
      alignment: Int
  ) extends PaddingResolver {

    require(alignment > 0, "alignment must be positive")

    override def resolve(context: PaddingContext): Int = {

      val recordContentSize = Math.addExact(
        context.headerSize.toLong,
        context.payloadLength
      )

      val remainder = recordContentSize % alignment

      ((alignment - remainder) % alignment).toInt
    }
  }

  /**
   * Resolve padding according to record status.
   *
   * Example:
   * Map(0 -> 0, 4 -> 1, 7 -> 3)
   */
  final case class ByStatus(
      paddingByStatus: Map[Int, Int],
      // defaultPadding: Option[Int] = None
      defaultPadding: Option[Int] = scala.None
  ) extends PaddingResolver {

    require(
      paddingByStatus.nonEmpty,
      "paddingByStatus cannot be empty"
    )

    paddingByStatus.foreach { case (status, padding) =>
      require(status >= 0 && status <= 15, s"Invalid status: $status")
      require(padding >= 0, "Padding cannot be negative")
    }

    defaultPadding.foreach { padding =>
      require(padding >= 0, "Default padding cannot be negative")
    }

    override def resolve(context: PaddingContext): Int = {

      paddingByStatus.get(context.status)
        .orElse(defaultPadding)
        .getOrElse {
          throw new IllegalArgumentException(
            s"No padding configured for status ${context.status}"
          )
        }
    }
  }
}
