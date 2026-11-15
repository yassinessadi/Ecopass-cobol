package io.github.yassinessadi.ecopass.spark

import io.github.yassinessadi.ecopass.framing.RecordFramingPolicy
import io.github.yassinessadi.ecopass.indexing.EcoPassPartition

import org.apache.spark.sql.connector.read.{
  Batch,
  InputPartition,
  PartitionReaderFactory
}

class EcoPassBatch(
    filePath: String,
    partitions: Vector[EcoPassPartition],
    framingPolicy: RecordFramingPolicy
) extends Batch {

  override def planInputPartitions(): Array[InputPartition] = {

    partitions.map { partition =>
      EcoPassInputPartition(
        filePath = filePath,
        partition = partition,
        framingPolicy = framingPolicy
      )
    }.toArray
  }

  override def createReaderFactory(): PartitionReaderFactory = {
    new EcoPassPartitionReaderFactory()
  }
}