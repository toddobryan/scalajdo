name := "scalajdo"

version := "0.1"

scalaVersion := "2.10.2"

libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % "2.10.2",
    "org.datanucleus" % "datanucleus-core" % "3.2.5",
    "org.datanucleus" % "datanucleus-rdbms" % "3.2.4",
    "org.datanucleus" % "datanucleus-api-jdo" % "3.2.4",
    "org.datanucleus" % "datanucleus-jdo-query" % "3.0.2",
    "javax.jdo" % "jdo-api" % "3.0.1",
    "org.scalatest" % "scalatest_2.10" % "2.0.M5b" % "test"
  )
