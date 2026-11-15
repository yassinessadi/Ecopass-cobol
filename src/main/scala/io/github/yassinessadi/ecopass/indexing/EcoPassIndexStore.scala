
package io.github.yassinessadi.ecopass.indexing

import java.io._
import java.nio.file.{Files, Path, StandardCopyOption}

/**
 * Persists EcoPass indexes in a binary sidecar file.
 *
 * The sidecar contains:
 * - Source file metadata
 * - Index and format versions
 * - Total record count
 * - Spark partition boundaries
 */
object EcoPassIndexStore {

  private val Magic: Int = 0x45504958 // "EPIX"
  private val CurrentVersion: Int = 1

  private val MaxPartitions: Int = 10000000

  /**
   * Generate a sidecar path.
   *
   * Example:
   * data.dat -> data.dat.epidx
   */
  def sidecarPath(sourcePath: String): Path = {
    java.nio.file.Paths.get(sourcePath + ".epidx")
  }

  /**
   * Save an index.
   */
  def write(index: EcoPassIndex, destination: Path): Unit = {

    val parent = destination.toAbsolutePath.getParent

    if (parent != null) {
      Files.createDirectories(parent)
    }

    val temporary = destination.resolveSibling(
      destination.getFileName.toString + ".tmp"
    )

    try {
      val output = new DataOutputStream(
        new BufferedOutputStream(
          Files.newOutputStream(temporary)
        )
      )

      try {
        output.writeInt(Magic)
        output.writeInt(CurrentVersion)

        output.writeUTF(index.sourcePath)
        output.writeLong(index.fileSize)
        output.writeLong(index.modificationTime)
        output.writeUTF(index.formatVersion)

        output.writeInt(index.indexVersion)
        output.writeLong(index.totalRecords)

        output.writeInt(index.partitions.size)

        index.partitions.foreach { partition =>
          output.writeInt(partition.partitionId)
          output.writeLong(partition.startOffset)
          output.writeLong(partition.endOffset)
          output.writeLong(partition.firstRecordIndex)
          output.writeLong(partition.recordCount)
        }

        output.flush()

      } finally {
        output.close()
      }

      try {
        Files.move(
          temporary,
          destination,
          StandardCopyOption.ATOMIC_MOVE,
          StandardCopyOption.REPLACE_EXISTING
        )
      } catch {
        case _: java.nio.file.AtomicMoveNotSupportedException =>
          Files.move(
            temporary,
            destination,
            StandardCopyOption.REPLACE_EXISTING
          )
      }

    } finally {
      Files.deleteIfExists(temporary)
    }
  }

  /**
   * Load an index from a sidecar file.
   */
  def read(source: Path): EcoPassIndex = {

    require(
      Files.exists(source),
      s"Index file does not exist: $source"
    )

    val input = new DataInputStream(
      new BufferedInputStream(
        Files.newInputStream(source)
      )
    )

    try {
      val magic = input.readInt()

      require(
        magic == Magic,
        "Invalid EcoPass index file"
      )

      val version = input.readInt()

      require(
        version == CurrentVersion,
        s"Unsupported index file version: $version"
      )

      val sourcePath = input.readUTF()
      val fileSize = input.readLong()
      val modificationTime = input.readLong()
      val formatVersion = input.readUTF()

      val indexVersion = input.readInt()
      val totalRecords = input.readLong()

      val partitionCount = input.readInt()

      require(
        partitionCount >= 0 && partitionCount <= MaxPartitions,
        s"Invalid partition count: $partitionCount"
      )

      val partitions = Vector.tabulate(partitionCount) { _ =>

        EcoPassPartition(
          partitionId = input.readInt(),
          startOffset = input.readLong(),
          endOffset = input.readLong(),
          firstRecordIndex = input.readLong(),
          recordCount = input.readLong()
        )
      }

      // Reject unexpected trailing bytes.
      require(
        input.read() == -1,
        "Unexpected trailing bytes in EcoPass index"
      )

      EcoPassIndex(
        sourcePath = sourcePath,
        fileSize = fileSize,
        modificationTime = modificationTime,
        formatVersion = formatVersion,
        indexVersion = indexVersion,
        totalRecords = totalRecords,
        partitions = partitions
      )

    } finally {
      input.close()
    }
  }

  /**
   * Check whether a sidecar exists.
   */
  def exists(source: Path): Boolean = {
    Files.exists(source)
  }

  /**
   * Load an index only if its source file metadata matches.
   */
  def readIfValid(
      indexPath: Path,
      currentFileSize: Long,
      currentModificationTime: Long
  ): Option[EcoPassIndex] = {

    if (!Files.exists(indexPath)) {
      return None
    }

    val index = read(indexPath)

    if (
      index.matches(
        currentFileSize,
        currentModificationTime
      )
    ) {
      Some(index)
    } else {
      None
    }
  }
}
