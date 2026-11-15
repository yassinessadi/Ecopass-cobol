package io.github.yassinessadi.ecopass.spark

import io.github.yassinessadi.ecopass.framing.RecordFramingPolicy
import io.github.yassinessadi.ecopass.indexing.EcoPassPartition

import org.apache.spark.sql.connector.read.{Batch, Scan}
import org.apache.spark.sql.types.StructType

class EcoPassScan(
    filePath: String,
    schema: StructType,
    partitions: Vector[EcoPassPartition],
    framingPolicy: RecordFramingPolicy
) extends Scan {

  override def readSchema(): StructType = schema

  override def description(): String = {
    s"EcoPassScan($filePath, partitions=${partitions.size})"
  }

  override def toBatch(): Batch = {
    new EcoPassBatch(
      filePath,
      partitions,
      framingPolicy
    )
  }
}