package io.github.yassinessadi.ecopass.spark

import org.apache.spark.sql.connector.catalog.Table
import org.apache.spark.sql.connector.catalog.TableProvider
import org.apache.spark.sql.connector.expressions.Transform
import org.apache.spark.sql.sources.DataSourceRegister
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.util.CaseInsensitiveStringMap

import java.util

class EcoPassDataSource
    extends TableProvider
    with DataSourceRegister {

  override def shortName(): String = "ecopass"

  override def inferSchema(
      options: CaseInsensitiveStringMap
  ): StructType = {
    EcoPassSchema.schema
  }

  override def supportsExternalMetadata(): Boolean = true

  override def getTable(
      schema: StructType,
      partitioning: Array[Transform],
      properties: util.Map[String, String]
  ): Table = {

    val effectiveSchema =
      if (schema == null) EcoPassSchema.schema
      else schema

    new EcoPassTable(effectiveSchema, properties)
  }
}