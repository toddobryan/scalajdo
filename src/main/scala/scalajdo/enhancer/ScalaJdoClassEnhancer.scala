package scalajdo.enhancer

import scala.collection.mutable
import org.datanucleus.metadata.{ AbstractMemberMetaData, ClassMetaData, PersistenceFlags }
import org.datanucleus.ClassLoaderResolver
import org.datanucleus.metadata.MetaDataManager
import javax.jdo.metadata.ClassPersistenceModifier
import org.datanucleus.enhancer.{ ClassField, ClassMethod }
import org.datanucleus.enhancer.AbstractClassEnhancer
import org.datanucleus.asm.Opcodes
import org.datanucleus.enhancer.spi.StateManager
import scalajdo.Util._
import org.datanucleus.enhancer.jdo.JDOEnhancementNamer

class ScalaJdoClassEnhancer(
    cmd: ClassMetaData,
    clr: ClassLoaderResolver,
    mmgr: MetaDataManager,
    val classBytes: Array[Byte]) {
  
  def enhance(): Boolean = {
    println(classDeclaration())
    true
  }
  
  lazy val namer = JDOEnhancementNamer.getInstance()
  
  lazy val fieldNames = cmd.getManagedMembers.map(m => s"_${m.getName}")
  lazy val fieldTypes = cmd.getManagedMembers.map(m => s"classOf[${typeFixer(m.getTypeName)}]")
  lazy val fieldFlags = cmd.getManagedMembers.map(m => persistenceFlagsToString(m.getPersistenceFlags))
  lazy val identityType = cmd.getIdentityType()
  lazy val objectIdClass = typeFixer(cmd.getObjectidClass())
  lazy val pkFieldNums = cmd.getPKMemberPositions()
  lazy val superClass = cmd.getPersistenceCapableSuperclass()
  lazy val pkg = cmd.getPackageName
  lazy val name = cmd.getName
  
  def classDeclaration(): String = {
    val superClassName = Option(superClass).getOrElse("PersistenceCapable")
    val detachable = if (this.requiresDetachable) " with Detachable" else ""
    s"""package $pkg
       |
       |class ${name} extends ${superClassName}${detachable} {
       |${generatedFields()}
       |${initializer()}
       |${if (superClass == null || cmd.isRootInstantiableClass()) rootPersistenceCapableMethods() else ""}
       |${if (!cmd.isAbstract()) jdoNewInstanceMethods() else ""}
       |}
       """
  }
  
  def companionObject(): String = {
    val inheritedFieldCount = cmd.getNoOfInheritedManagedMembers()
    val managedMembers = cmd.getManagedMembers()
    val superClassString = if (superClass == null) "null" else s"classOf[${typeFixer(superClass)}]"
    s"""object ${name} extends PersistenceCapable.Statics {
       |  protected val jdoInheritedFieldCount: Int = $inheritedFieldCount
       |  protected val jdoFieldNames: Array[String] = Array(${fieldNames.map(repr(_)).mkString(", ")})
       |  protected val jdoFieldTypes: Array[Class[_]] = Array(${fieldTypes.mkString(", ")})
       |  protected val jdoFieldFlags: Array[Byte] = Array(${fieldFlags.mkString("\n      ", ",\n      ", "")}
       |  )
       |  protected val jdoPersistenceCapableSuperclass: Class[_] = $superClassString
       """.stripMargin
  }
  
  def generatedFields(): String = {
    s"""  @transient
       |  protected var jdoStateManager: StateManager = null
       |
       |  @transient
       |  protected var jdoFlags: Byte = _
       """.stripMargin
  }
  
  def initializer(): String = {
    val helperInstance = if (cmd.isAbstract()) "null" else s"new ${cmd.getName}()"
    s"""  JDOImplHelper.registerClass(
       |      classOf[${cmd.getName}],
       |      jdoFieldNames,
       |      jdoFieldTypes,
       |      jdoFieldFlags,
       |      jdoPersistenceCapableSuperclass,
       |      ${helperInstance}
       |  )
    """.stripMargin
  }
  
  def jdoNewInstanceMethods(): String = {
    s"""  def jdoNewInstance(sm: StateManager): javax.jdo.spi.PersistenceCapable = {
       |    val newObj: ${name} = new ${name}()
       |    newObj.jdoFlags = LOAD_REQUIRED
       |    newObj.jdoStateManager = sm
       |    newObj
       |  }
       |
       |  def jdoNewInstance(sm: StateManager, obj: Object): javax.jdo.spi.PersistenceCapable = {
       |    val newObj = jdoNewInstance(sm)
       |    newObj.jdoCopyKeyFieldsFromObjectId(obj)
       |    newObj
       |  }
    """
  }
  
  // TODO: only works for single-key application identity at the moment
  def rootPersistenceCapableMethods(): String = {
    val oidType = cmd.getObjectidClass()
    val idField = cmd.getPKMemberPositions()(0)
    val fieldType = namer.getTypeNameForUseWithSingleFieldIdentity(oidType)
    s"""  def jdoCopyKeyFieldsToObjectId(Object oid) {
       |    throw new JDOFatalInternalException("It's illegal to call jdoCopyKeyFieldsToObjectId for a class with SingleFieldIdentity.")
       |  }
       |
       |  def jdoCopyKeyFieldsToObjectId(fs: ObjectIdFieldSupplier, paramObject: Object) {
       |    throw new JDOFatalInternalException("It's illegal to call jdoCopyKeyFieldsToObjectId for a class with SingleFieldIdentity.")
       |  }
       |
       |  protected def jdoCopyKeyFieldsFromObjectId(Object oid) {
       |    if (!oid.isInstanceOf[${oidType}])
       |      throw new ClassCastException("key class is not ${oidType} or null");
       |    } else {
       |      this.${fieldNames(idField)} = oid.asInstanceOf[${oidType}]
       |    }
       |  }
       |
       |  protected def jdoCopyKeyFieldsFromObjectId(fc: ObjectIdFieldConsumer, oid: Object) {
       |    if (fc == null) throw new IllegalArgumentException("ObjectIdFieldConsumer is null")
       |    else if (!oid.isInstanceOf[${oidType}]) throw new ClassCastException("oid is not an instance of ${oidType}")
       |    else fc.store${fieldType}Field(${idField}, oid.asInstanceOf[${oidType}].getKey)
       |  }
       |
       |  def jdoNewObjectIdInstance(): Object = {
       |    new ${oidType}(getClass(), this.${fieldNames(idField)})
       |  }
       |
       |  def jdoNewObjectIdInstance(Object obj): Object = {
       |    if (key == null) throw new IllegalArgumentException("key is null");
       |    else if (!key.isInstanceOf[String]) new ${oidType}(getClass(), key.asInstanceOf[${fieldType}])
       |    else new ${oidType}(getClass(), key.asInstanceOf[String])
       |  }
     """.stripMargin
  }
  
  protected def requiresDetachable(): Boolean = {
    // this class is detachable and either has no super class or has one that isn't, itself, detachable
    cmd.isDetachable() && (superClass == null || !cmd.getSuperAbstractClassMetaData().isDetachable())
  }
  
  def getterAndSetter(md: AbstractMemberMetaData): String = {
    val fieldName = md.getName()
    val fieldType = md.getTypeName()
    s"""  private[this] var _${fieldName}: ${standardConversions(fieldType)} = _
       |  private[this] def _${fieldName}: ${fieldType} = { ${accessorBody(md)} }
       |  private[this] def _${fieldName}_=(newVal: ${fieldType}) { ${mutatorBody(md)} }
    """.stripMargin
  }
  
  def accessorBody(md: AbstractMemberMetaData): String = {
    /*val jdoFlag = md.getPersistenceFlags()
    if ((jdoFlag & PersistenceFlags.MEDIATE_READ) != 0) {
      accessorViaMediate(md)
    } else if ((jdoFlag & PersistenceFlags.CHECK_READ) != 0) {
      accessorViaCheck(md)
    } else {
      accessorNormal(md)
    }*/
    ""
  }
  
  def mutatorBody(md: AbstractMemberMetaData): String = ""  
  
  def standardConversions(fieldType: String): String = fieldType match {
    case "java.sql.Date" => "org.joda.time.LocalDate"
    case _ => fieldType
  }
  
  
}