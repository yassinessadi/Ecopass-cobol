
package io.github.yassinessadi.ecopass.framing

/**
 * Determines the header size for a record.
 *
 * The first byte is supplied as an unsigned integer (0-255).
 * Its upper 4 bits contain the record status.
 */
trait HeaderFormatResolver extends Serializable {

  def resolve(
      recordOffset: Long,
      firstByte: Int
  ): Int
}

object HeaderFormatResolver {

  private def validateHeaderSize(size: Int): Int = {
    require(
      size == 2 || size == 4,
      s"Unsupported header size: $size. Expected 2 or 4."
    )
    size
  }

  /**
   * Use one header size for every record.
   */
  final case class Fixed(headerSize: Int)
      extends HeaderFormatResolver {

    validateHeaderSize(headerSize)

    override def resolve(
        recordOffset: Long,
        firstByte: Int
    ): Int = headerSize
  }

  /**
   * Resolve header size using the record status.
   *
   * Example:
   * Map(0 -> 2, 4 -> 2, 7 -> 4)
   *
   * This is valid only if each status always uses the same
   * header size in the source format.
   */
  final case class ByStatus(
      headerSizes: Map[Int, Int],
      defaultHeaderSize: Option[Int] = None
  ) extends HeaderFormatResolver {

    require(headerSizes.nonEmpty, "headerSizes cannot be empty")

    headerSizes.foreach { case (status, size) =>
      require(status >= 0 && status <= 15, s"Invalid status: $status")
      validateHeaderSize(size)
    }

    defaultHeaderSize.foreach(validateHeaderSize)

    override def resolve(
        recordOffset: Long,
        firstByte: Int
    ): Int = {

      val status = (firstByte >>> 4) & 0x0F

      headerSizes.get(status)
        .orElse(defaultHeaderSize)
        .getOrElse {
          throw new IllegalArgumentException(
            s"No header size configured for status $status at offset $recordOffset"
          )
        }
    }
  }

  /**
   * Resolve header size using configured file-offset ranges.
   *
   * Each range is [startOffset, endOffset).
   *
   * This is useful only when the file format defines header
   * size by a known region of the file.
   */
  final case class ByOffset(
      ranges: Vector[HeaderSizeRange]
  ) extends HeaderFormatResolver {

    require(ranges.nonEmpty, "At least one range is required")

    private val sortedRanges = ranges.sortBy(_.startOffset)

    // sortedRanges.foreach(validateHeaderSize(_.headerSize))
    sortedRanges.foreach { range =>
      validateHeaderSize(range.headerSize)
    }

    sortedRanges.sliding(2).foreach {
      case Vector(left, right) =>
        require(
          left.endOffset <= right.startOffset,
          "Header size ranges cannot overlap"
        )
      case _ =>
    }

    override def resolve(
        recordOffset: Long,
        firstByte: Int
    ): Int = {

      sortedRanges.find { range =>
        recordOffset >= range.startOffset &&
        recordOffset < range.endOffset
      }.map(_.headerSize).getOrElse {
        throw new IllegalArgumentException(
          s"No header size configured at offset $recordOffset"
        )
      }
    }
  }
}

final case class HeaderSizeRange(
    startOffset: Long,
    endOffset: Long,
    headerSize: Int
) extends Serializable {

  require(startOffset >= 0L, "startOffset cannot be negative")
  require(endOffset > startOffset, "endOffset must be greater than startOffset")
  require(
    headerSize == 2 || headerSize == 4,
    "headerSize must be 2 or 4"
  )
}
