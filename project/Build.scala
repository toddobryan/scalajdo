import sbt._
import Keys._

import java.io.File

object ScalaJdoBuild extends Build {
  val dependencies = Seq(
    "org.scala-lang" % "scala-reflect" % "2.10.2",
    "org.datanucleus" % "datanucleus-core" % "3.2.6",
    "org.datanucleus" % "datanucleus-rdbms" % "3.2.5",
    "org.datanucleus" % "datanucleus-api-jdo" % "3.2.4",
    "org.datanucleus" % "datanucleus-jdo-query" % "3.0.2",
    "javax.jdo" % "jdo-api" % "3.0.1",
    "javax.transaction" % "jta" % "1.1",
    "org.scalatest" % "scalatest_2.10" % "2.0.M5b" % "test",
    "log4j" % "log4j" % "1.2.17" % "test",
    "com.h2database" % "h2" % "1.3.172" % "test"  
  )
  
  val publishing = Seq(
    publishMavenStyle := true,
    publishTo <<= version { (v: String) =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT")) {
        Some("snapshots" at nexus + "content/repositories/snapshots")
      } else {
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
      }
    },
    publishArtifact in Test := false,
    credentials += Credentials(Path.userHome / ".ssh" / ".credentials"),
    pomExtra := (
      <url>http://dupontmanual.github.io/dm-forms</url>
	  <licenses>
        <license>
          <name>Apache 2</name>
          <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
        </license>
      </licenses>
      <scm>
        <url>git://github.com/dupontmanual/scalajdo.git</url>
        <connection>scm:git://github.com/dupontmanual/scalajdo.git</connection>
      </scm>
      <organization>
        <name>duPont Manual High School Computer Science Department</name>
        <url>http://dupontmanual.github.io/</url>
      </organization>
      <developers>
        <developer>
          <name>Allen Boss</name>
          <roles>
            <role>Student, Class of 2013</role>
          </roles>
        </developer>
        <developer>
          <name>Todd O'Bryan</name>
          <roles>
            <role>Teacher</role>
          </roles>
        </developer>
      </developers>
    )
  )
  
  val buildSettings = Defaults.defaultSettings ++ Seq(
    name := "Scala JDO",
    normalizedName := "scalajdo",
    description := "a project to simplify use of the DataNucleus JDO library from Scala",
    organization := "org.dupontmanual",
    organizationName := "duPont Manual High School",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.10.2",
    parallelExecution in Test := false,
    testOptions in Test += Tests.Argument("-oDF"),
    //fork in Test := true,
    libraryDependencies ++= dependencies
  ) 
  
  lazy val scalajdo = Project(
      "scalajdo", 
      file("."),
      settings = buildSettings ++
        publishing ++ 
        Nucleus.settings
  )
}

object Nucleus {
  // defines our own ivy config that wont get packaged as part of your app
  // notice that it extends the Compile scope, so we inherit that classpath
  val Config = config("nucleus") extend Compile

  // our task
  val enhance = TaskKey[Unit]("enhance")

  val settings: Seq[Project.Setting[_]] = Seq(
    ivyConfigurations += Config,
  /*
  // implementation
  val settings: Seq[Project.Setting[_]] = Seq(
    // let ivy know about our "nucleus" config
    ivyConfigurations += Config,
    // add the enhancer dependency to our nucleus ivy config
    libraryDependencies += "org.datanucleus" % "datanucleus-core" % "3.2.4" % Config.name,
    // fetch the classpath for our nucleus config
    // as we inherit Compile this will be the fullClasspath for Compile + "datanucleus-enhancer" jar 
    //fullClasspath in Config <<= (classpathTypes in enhance, update).map{(ct, report) =>
    //  Classpaths.managedJars(Config, ct, report)
    //},
    // add more parameters as your see fit
    //enhance in Config <<= (fullClasspath in Config, runner, streams).map{(cp, run, s) =>
     */
    enhance <<= Seq(compile in Compile).dependOn,
    enhance in Config <<= (fullClasspath in Test, runner, streams) map { (cp, run, s) => 
      val options = Seq("-v", "-pu", "scalajdo.examples")
      val result = run.run("org.datanucleus.enhancer.DataNucleusEnhancer", cp.files, options, s.log)
      result.foreach(sys.error)
    })
    /* enhance in Config <<= 
      (fullClasspath in Test, 
       classDirectory in Test, 
       runner, streams)
      map { (cp, mainClasses, run, s) =>

        // Properties
        val classpath = cp.files
        enhanceClasses(run, classpath, mainClasses, s) */

        /*// the classpath is attributed, we only want the files
        //val classpath = cp.files
        // the options passed to the Enhancer... 
        val mainOptions = Seq("-v") ++ findAllClassesRecursively(mainClasses).map(_.getAbsolutePath)

        // run returns an option of errormessage
        val mainResult = run.run("org.datanucleus.enhancer.DataNucleusEnhancer", classpath, mainOptions, s.log)
        // if there is an errormessage, throw an exception
        mainResult.foreach(sys.error)
        
        val userOptions = Seq("-v") ++ findAllClassesRecursively(userClasses).map(_.getAbsolutePath)
        
        val usersResult = run.run("org.datanucleus.enhancer.DataNucleusEnhancer", classpath, userOptions, s.log)
        usersResult.foreach(sys.error) */   
      
  def enhanceClasses(runner: ScalaRun, classpath: Seq[File], classes: File, streams: TaskStreams) = {
    val options = Seq("-v") ++ findAllClassesRecursively(classes).map(_.getAbsolutePath)
    val result = runner.run("org.datanucleus.enhancer.DataNucleusEnhancer", classpath, options, streams.log)
    result.foreach(sys.error)
  }

      
  def findAllClassesRecursively(dir: File): Seq[File] = {
    if (dir.isDirectory) {
      val files = dir.listFiles
      files.flatMap(findAllClassesRecursively(_))
    } else if (dir.getName.endsWith(".class")) {
      Seq(dir)
    } else {
      Seq.empty
    }
  }
}