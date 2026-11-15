
package io.github.yassinessadi.ecopass.core

final case class EcoPassRecord(
    status: Int,
    length: Long,
    data: Array[Byte],
    offset: Long = 0L,
    headerSize: Int = 2,
    paddingSize: Int = 0
) extends Serializable {

  require(status >= 0 && status <= 15, "Status must be between 0 and 15")
  require(length >= 0L, "Record length cannot be negative")
  require(data != null, "Record data cannot be null")
  require(length == data.length.toLong, "Length must match payload size")
  require(offset >= 0L, "Record offset cannot be negative")
  require(headerSize == 2 || headerSize == 4, "Header size must be 2 or 4")
  require(paddingSize >= 0, "Padding size cannot be negative")
}
