package io.github.yassinessadi.ecopass.spark

import io.github.yassinessadi.ecopass.indexing.EcoPassPartition
import io.github.yassinessadi.ecopass.framing.RecordFramingPolicy

import org.apache.spark.sql.connector.read.InputPartition

final case class EcoPassInputPartition(
    filePath: String,
    partition: EcoPassPartition,
    framingPolicy: RecordFramingPolicy
) extends InputPartition