
package io.github.yassinessadi.ecopass.indexing

import scala.collection.mutable.ArrayBuffer

object EcoPassIndexBuilder {

  val DefaultTargetPartitionBytes: Long = 256L * 1024L * 1024L

  val CurrentIndexVersion: Int = 1

  /**
   * Build an index from verified record boundaries.
   *
   * The boundaries must be supplied in file order.
   * Each boundary must include its padding in endOffset.
   */
  def build(
      sourcePath: String,
      fileSize: Long,
      modificationTime: Long,
      boundaries: Iterator[RecordBoundary],
      targetPartitionBytes: Long = DefaultTargetPartitionBytes,
      formatVersion: String = "1"
  ): EcoPassIndex = {

    require(fileSize >= 0L, "fileSize cannot be negative")
    require(
      targetPartitionBytes > 0L,
      "targetPartitionBytes must be positive"
    )

    val partitions = ArrayBuffer.empty[EcoPassPartition]

    var partitionStart = 0L
    var partitionEnd = 0L
    var partitionFirstRecord = 0L
    var partitionRecordCount = 0L

    var totalRecords = 0L
    var previousEnd = 0L

    def closePartition(): Unit = {
      if (partitionRecordCount > 0L) {
        partitions += EcoPassPartition(
          partitionId = partitions.size,
          startOffset = partitionStart,
          endOffset = partitionEnd,
          firstRecordIndex = partitionFirstRecord,
          recordCount = partitionRecordCount
        )
      }
    }

    boundaries.foreach { boundary =>

      require(
        boundary.startOffset == previousEnd,
        s"Non-contiguous or unordered record boundary at ${boundary.startOffset}; expected $previousEnd"
      )

      require(
        boundary.endOffset <= fileSize,
        s"Record at ${boundary.startOffset} exceeds file size $fileSize"
      )

      if (partitionRecordCount == 0L) {
        partitionStart = boundary.startOffset
        partitionFirstRecord = totalRecords
      }

      partitionEnd = boundary.endOffset
      partitionRecordCount += 1L
      totalRecords += 1L
      previousEnd = boundary.endOffset

      val partitionSize = partitionEnd - partitionStart

      // Close after a complete record reaches or exceeds the target.
      if (partitionSize >= targetPartitionBytes) {
        closePartition()
        partitionRecordCount = 0L
      }
    }

    // Add the final partial partition.
    closePartition()

    require(
      previousEnd == fileSize,
      s"Indexed records end at $previousEnd but file size is $fileSize"
    )

    EcoPassIndex(
      sourcePath = sourcePath,
      fileSize = fileSize,
      modificationTime = modificationTime,
      formatVersion = formatVersion,
      indexVersion = CurrentIndexVersion,
      totalRecords = totalRecords,
      partitions = partitions.toVector
    )
  }
}
