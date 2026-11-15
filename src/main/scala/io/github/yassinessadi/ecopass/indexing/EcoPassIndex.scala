
package io.github.yassinessadi.ecopass.indexing

/**
 * A Spark-readable byte range containing complete records.
 *
 * endOffset is exclusive.
 */
final case class EcoPassPartition(
    partitionId: Int,
    startOffset: Long,
    endOffset: Long,
    firstRecordIndex: Long,
    recordCount: Long
) extends Serializable {

  require(partitionId >= 0, "partitionId cannot be negative")
  require(startOffset >= 0L, "startOffset cannot be negative")
  require(endOffset >= startOffset, "endOffset cannot be before startOffset")
  require(firstRecordIndex >= 0L, "firstRecordIndex cannot be negative")
  require(recordCount >= 0L, "recordCount cannot be negative")

  def length: Long = endOffset - startOffset
}

/**
 * Metadata describing a scanned EcoPass file.
 *
 * The index stores partition boundaries, not every record offset.
 * This keeps the index relatively small for large files.
 */
final case class EcoPassIndex(
    sourcePath: String,
    fileSize: Long,
    modificationTime: Long,
    formatVersion: String,
    indexVersion: Int,
    totalRecords: Long,
    partitions: Vector[EcoPassPartition]
) extends Serializable {

  require(sourcePath != null && sourcePath.nonEmpty, "sourcePath is required")
  require(fileSize >= 0L, "fileSize cannot be negative")
  require(modificationTime >= 0L, "modificationTime cannot be negative")
  require(formatVersion != null && formatVersion.nonEmpty, "formatVersion is required")
  require(indexVersion > 0, "indexVersion must be positive")
  require(totalRecords >= 0L, "totalRecords cannot be negative")

  validatePartitions()

  def partitionCount: Int = partitions.size

  /**
   * Check whether the source file still matches this index metadata.
   */
  def matches(
      currentFileSize: Long,
      currentModificationTime: Long
  ): Boolean = {
    fileSize == currentFileSize &&
    modificationTime == currentModificationTime
  }

  private def validatePartitions(): Unit = {
    var expectedStart = 0L
    var expectedRecordIndex = 0L

    partitions.zipWithIndex.foreach { case (partition, index) =>

      require(
        partition.partitionId == index,
        s"Unexpected partition ID: ${partition.partitionId}"
      )

      require(
        partition.startOffset == expectedStart,
        s"Partition $index does not start at the expected offset"
      )

      require(
        partition.firstRecordIndex == expectedRecordIndex,
        s"Partition $index has an unexpected first record index"
      )

      require(
        partition.endOffset <= fileSize,
        s"Partition $index exceeds the source file size"
      )

      expectedStart = partition.endOffset
      expectedRecordIndex += partition.recordCount
    }

    require(
      expectedStart == fileSize || fileSize == 0L,
      "Partitions do not cover the complete source file"
    )

    require(
      expectedRecordIndex == totalRecords,
      "Partition record counts do not match totalRecords"
    )

    if (fileSize == 0L) {
      require(partitions.isEmpty, "An empty file must not have partitions")
    }
  }
}
