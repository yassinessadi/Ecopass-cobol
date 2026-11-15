package io.github.yassinessadi.ecopass.spark

import org.apache.spark.sql.connector.read.{
  InputPartition,
  PartitionReader,
  PartitionReaderFactory
}

import org.apache.spark.sql.catalyst.InternalRow

class EcoPassPartitionReaderFactory
    extends PartitionReaderFactory {

  override def createReader(
      partition: InputPartition
  ): PartitionReader[InternalRow] = {

    val input = partition.asInstanceOf[EcoPassInputPartition]

    new EcoPassPartitionReader(input)
  }
}