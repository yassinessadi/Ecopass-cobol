package io.github.yassinessadi.ecopass.spark

import org.apache.spark.sql.connector.catalog.{SupportsRead, Table, TableCapability}
import org.apache.spark.sql.connector.read.ScanBuilder
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.util.CaseInsensitiveStringMap

import java.util

import scala.collection.JavaConverters._

class EcoPassTable(
    tableSchema: StructType,
    properties: util.Map[String, String]
) extends Table
    with SupportsRead {

  override def name(): String = "EcoPassTable"

  override def schema(): StructType = tableSchema

  override def capabilities(): util.Set[TableCapability] = {
    Set(TableCapability.BATCH_READ).asJava
  }

  override def newScanBuilder(
      options: CaseInsensitiveStringMap
  ): ScanBuilder = {

    new EcoPassScanBuilder(
      tableSchema,
      options
    )
  }
}