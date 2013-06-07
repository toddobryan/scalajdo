name := "scalajdo"

version := "0.1"

scalaVersion := "2.10.0"

libraryDependencies ++= Seq("javax.jdo" % "jdo-api" % "3.0",
    "org.datanucleus" % "datanucleus-core" % "3.1.3",
    "org.datanucleus" % "datanucleus-api-jdo" % "3.1.3",
    "org.datanucleus" % "datanucleus-enhancer" % "3.1.1",
    "org.datanucleus" % "datanucleus-jdo-query" % "3.0.2"
  )
