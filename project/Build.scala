import sbt._
import Keys._

import java.io.File

object ApplicationBuild extends Build {
  val appName = "scalajdo"
  
  lazy val scalajdo = Project(
      appName, 
      file("."),
      settings = Defaults.defaultSettings ++ Seq(
          organization := "dupontmanual",
          version := "0.1",
          scalaVersion := "2.10.2",
          javacOptions ++= Seq("-source", "1.6", "-target", "1.6", "-bootclasspath", "/usr/lib/jvm/java-6-oracle/jre/lib/rt.jar"),
          scalacOptions ++= Seq("-deprecation", "-feature"),
          libraryDependencies ++= Seq(
              "org.scala-lang" % "scala-library" % "2.10.2",
              "org.scala-lang" % "scala-reflect" % "2.10.2",
              //"org.datanucleus" % "datanucleus-core" % "3.2.3",
              //"org.datanucleus" % "datanucleus-rdbms" % "3.2.3",
              //"org.datanucleus" % "datanucleus-api-jdo" % "3.2.3",
              "org.datanucleus" % "datanucleus-jdo-query" % "3.0.2",
              //"javax.jdo" % "jdo-api" % "3.0.1",
              "javax.transaction" % "jta" % "1.1",
              "org.scalatest" % "scalatest_2.10" % "2.0.M5b" % "test",
              "log4j" % "log4j" % "1.2.17" % "test",
              "com.h2database" % "h2" % "1.3.172" % "test"
          )
      ) ++ Nucleus.settings
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