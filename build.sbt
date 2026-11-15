
ThisBuild / organization := "io.github.yassinessadi"
ThisBuild / version := "0.1.0"
ThisBuild / scalaVersion := "2.12.15"

lazy val root = (project in file("."))
  .settings(
    name := "ecopass",

    libraryDependencies ++= Seq(
      // Spark 3.3.3
      "org.apache.spark" %% "spark-sql" % "3.3.3" % Provided,

      // Hadoop APIs
      "org.apache.hadoop" % "hadoop-common" % "3.3.2" % Provided,
      "org.apache.hadoop" % "hadoop-hdfs-client" % "3.3.2" % Provided,

      // Unit testing
      "org.scalatest" %% "scalatest" % "3.2.18" % Test
    ),

    Compile / scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Xlint"
    ),

    Compile / packageBin / packageOptions +=
      Package.ManifestAttributes(
        "Implementation-Title" -> "EcoPass",
        "Implementation-Version" -> version.value
      ),

    Test / parallelExecution := false
  )
