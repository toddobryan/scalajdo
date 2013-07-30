addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.2.0")

libraryDependencies ++= Seq(
    "javax.jdo" % "jdo-api" % "3.0.1",
    "org.datanucleus" % "datanucleus-core" % "3.2.3",
    "org.datanucleus" % "datanucleus-api-jdo" % "3.2.3"
)
