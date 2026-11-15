
package io.github.yassinessadi.ecopass.indexing

/**
 * A verified physical record boundary in the source file.
 *
 * endOffset is exclusive and includes the record's padding.
 */
final case class RecordBoundary(
    startOffset: Long,
    endOffset: Long,
    status: Int,
    headerSize: Int,
    payloadLength: Long,
    paddingSize: Int
) extends Serializable {

  require(startOffset >= 0L, "startOffset cannot be negative")
  require(endOffset > startOffset, "endOffset must be greater than startOffset")
  require(status >= 0 && status <= 15, "status must fit in 4 bits")
  require(headerSize == 2 || headerSize == 4, "headerSize must be 2 or 4")
  require(payloadLength >= 0L, "payloadLength cannot be negative")
  require(paddingSize >= 0, "paddingSize cannot be negative")

  def totalSize: Long = endOffset - startOffset

  def payloadOffset: Long = startOffset + headerSize

  def payloadEndOffset: Long = payloadOffset + payloadLength
}
