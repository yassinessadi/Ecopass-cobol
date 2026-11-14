package io.github.yassinessadi.ecopass

/**
 * One decoded record. data contains only the record payload, without header or padding.
 */
final case class EcoPassRecord(
  status: Int,
  length: Int,
  data: Array[Byte],
  offset: Long
) extends Serializable {
  def recordTypeName: String = RecordType.nameOf(status)
}
