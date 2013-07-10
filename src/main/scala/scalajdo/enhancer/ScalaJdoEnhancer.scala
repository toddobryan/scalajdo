package scalajdo.enhancer

class ScalaJdoEnhancer {
  
  def this(filenames: List[String]) {
    this()
    val (classFiles, rest) = filenames.partition(_.endsWith(".class"))
    val (jarFiles, mappingFiles) = rest.partition(_.endsWith(".jar"))
    val 
    val componentsToEnhance: List[EnhanceComponent] = 
}

object ScalaJdoEnhancer extends App {
  val enhancer = new ScalaJdoEnhancer(args.toList)
}

sealed abstract class EnhanceComponent
case class ClassComponent(name: String, bytes: Array[Byte]) extends EnhanceComponent

