
ThisBuild / organization := "io.github.yassinessadi"
ThisBuild / version := "0.1.0"
ThisBuild / scalaVersion := "2.12.15"

lazy val root = (project in file("."))
  .settings(
    name := "ecopass",

    libraryDependencies ++= Seq(
      // Spark dependency
      "org.apache.spark" %% "spark-sql" % "3.3.3" % Provided,

      // Unit testing
      "org.scalatest" %% "scalatest" % "3.2.18" % Test
    ),

    Compile / packageBin / packageOptions +=
      Package.ManifestAttributes(
        "Implementation-Title" -> "EcoPass",
        "Implementation-Version" -> version.value
      ),

    Test / parallelExecution := false
  )
