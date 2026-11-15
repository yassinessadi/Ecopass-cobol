package io.github.yassinessadi.ecopass.spark

import org.apache.spark.sql.types._

object EcoPassSchema {

  val schema: StructType = StructType(
    Seq(
      StructField("status", IntegerType, nullable = false),
      StructField("record_type", StringType, nullable = false),
      StructField("length", LongType, nullable = false),
      StructField("data", BinaryType, nullable = false),
      StructField("record_offset", LongType, nullable = false),
      StructField("header_size", IntegerType, nullable = false),
      StructField("padding_size", IntegerType, nullable = false)
    )
  )
}