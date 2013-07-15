package scalajdo.enhancer

import java.io.File
import scala.collection.JavaConverters._
import scala.collection.mutable
import org.datanucleus.util.ClassUtils
import java.util.{ Collection, Properties }
import org.datanucleus.metadata.FileMetaData
import org.datanucleus.metadata.MetaDataManager
import org.datanucleus.NucleusContext
import org.datanucleus.ClassLoaderResolver
import org.datanucleus.metadata.PersistenceUnitMetaData
import org.datanucleus.metadata.ClassMetaData
import org.datanucleus.enhancer.jdo.JDOClassEnhancer

sealed abstract class EnhanceComponent
case class ClassComponent(name: String, bytes: Array[Byte]) extends EnhanceComponent
case class ClassFileComponent(files: Seq[File]) extends EnhanceComponent
case class MappingFileComponent(files: Seq[File]) extends EnhanceComponent
case class JarFileComponent(files: Seq[File]) extends EnhanceComponent
case class PersistenceUnitComponent(name: String) extends EnhanceComponent

case class Config(
    persistenceUnitName: Option[String] = None, 
    directory: Option[File] = None, 
    files: Seq[File] = Seq())

class ScalaJdoEnhancer(val config: Config, val componentsToEnhance: Seq[EnhanceComponent], val props: Properties) {
  def this(config: Config, props: Properties) {
    this(config, ScalaJdoEnhancer.buildComponents(config), props)
  }
  
  lazy val nucleusContext: NucleusContext = new NucleusContext("JDO", NucleusContext.ContextType.ENHANCEMENT, props)

  lazy val metadataMgr: MetaDataManager = {
    if (props != null) nucleusContext.getPersistenceConfiguration().setPersistenceProperties(props)
    nucleusContext.getMetaDataManager()
  }
  
  lazy val clr: ClassLoaderResolver = nucleusContext.getClassLoaderResolver(null)
  
  lazy val userClassLoader = null
  
  def getFileMetadataForInput(componentsToEnhance: Seq[EnhanceComponent]): Seq[FileMetaData] = {
    componentsToEnhance.flatMap(comp => comp match {
      case ClassComponent(name, bytes) => Seq() // TODO: finish
      case ClassFileComponent(files) => {
        metadataMgr.loadClasses(files.map(f => JDOClassEnhancer.getClassNameForFileName(f.getCanonicalPath())).toArray, userClassLoader).toList
      }
      case MappingFileComponent(files) => {
        metadataMgr.loadMetadataFiles(files.map(_.getName).toArray, userClassLoader).toList
      }
      case JarFileComponent(files) => {
        files.flatMap(jar => metadataMgr.loadJar(jar.getName, userClassLoader))
      }
      case PersistenceUnitComponent(name) =>
        val pumd: PersistenceUnitMetaData = metadataMgr.getMetaDataForPersistenceUnit(name)
        metadataMgr.loadPersistenceUnit(pumd, userClassLoader).toList
    })
  }
  
  def getClassEnhancer(cmd: ClassMetaData, bytes: Array[Byte]) = {
    new ScalaJdoClassEnhancer(cmd, clr, metadataMgr, bytes)
  }
  
  def enhanceClass(cmd: ClassMetaData, enhancer: ScalaJdoClassEnhancer, store: Boolean): Boolean = {
    // TODO: really incomplete
    enhancer.enhance()
  }
  
  def run() {
    var success: Boolean = true
    val classNames = mutable.Set[String]()
    getFileMetadataForInput(componentsToEnhance).foreach { filemd =>
      (0 until filemd.getNoOfPackages()).foreach { packageNum =>
        val pmd = filemd.getPackage(packageNum)
        (0 until pmd.getNoOfClasses()).foreach { classNum =>
          val cmd = pmd.getClass(classNum)
          if (!classNames.contains(cmd.getFullClassName())) {
            classNames.add(cmd.getFullClassName())
            val bytes = null // TODO: need to fix for classes
            val classEnhancer = getClassEnhancer(cmd, bytes)
            val clsSuccess: Boolean = enhanceClass(cmd, classEnhancer, bytes == null)
            if (!clsSuccess) success = false
          }
        }
      }  
    }
  }
}

object ScalaJdoEnhancer extends App {
  val parser = new scopt.OptionParser[Config]("enhance") {
    opt[String]('p', "persistenceUnit") action { (name, config) => 
      config.copy(persistenceUnitName = Some(name)) } text("the name of the persistence unit to enhance")
    opt[File]('d', "directory") action { (dir, config) =>
      config.copy(directory = Some(dir)) } text("the directory holding the class files to enhance")
    arg[File]("<file>...") optional() unbounded() action { (file, config) =>
      config.copy(files = config.files :+ file) } text("the class files to enhance")
  }
  
  parser.parse(args, Config()) map { config =>
    val props = new Properties()
    props.setProperty("datanucleus.plugin.allowUserBundles", "true")
    new ScalaJdoEnhancer(config, props).run()  
  } getOrElse {
    println("Oops!")
  }
  
  // Auxiliary functions
  def buildComponents(conf: Config): Seq[EnhanceComponent] = {
    if (conf.persistenceUnitName.isDefined) {
      Seq(PersistenceUnitComponent(conf.persistenceUnitName.get))
    } else if (conf.directory.isDefined) {
      val dir = conf.directory.get
      if (!dir.exists() || !dir.isDirectory()) {
        println(s"${dir.getName} is not a directory")
        sys.exit(1)
      } else {
        componentsFromFiles(ClassUtils.getFilesForDirectory(dir))
      }
    } else {
      componentsFromFiles(conf.files)
    }
  }
  
  def componentsFromFiles(files: Collection[File]): Seq[EnhanceComponent] = {
    componentsFromFiles(files.asScala.toList)
  }
  
  def componentsFromFiles(files: Seq[File]): Seq[EnhanceComponent] = {
    val (classFiles, rest) = files.partition(_.getName.endsWith(".class"))
    val (jarFiles, mappingFiles) = rest.partition(_.getName.endsWith(".jar"))
    val mappingFileSeq = if (mappingFiles.isEmpty) Seq()
        else Seq(MappingFileComponent(mappingFiles))
    val jarFileSeq = if (jarFiles.isEmpty) Seq()
        else Seq(JarFileComponent(jarFiles))
    val classFileSeq = if (classFiles.isEmpty) Seq()
        else Seq(ClassFileComponent(classFiles))
    val comps = mappingFileSeq ++ jarFileSeq ++ classFileSeq
    println(comps)
    comps
  }
}


