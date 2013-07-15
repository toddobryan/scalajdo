package scalajdo.examples.enhanced

import scalajdo.PersistenceCapable
import scalajdo.Detachable
import org.datanucleus.metadata.PersistenceFlags
import javax.jdo.JDOFatalInternalException
import javax.jdo.spi.PersistenceCapable.ObjectIdFieldSupplier
import javax.jdo.spi.PersistenceCapable.ObjectIdFieldConsumer
import scalajdo.examples.Department
import org.datanucleus.metadata.IdentityType

class Employee extends PersistenceCapable[Employee] with Detachable[Employee] {
  val companion = Statics
    
  private var _boss: Employee = _
  private var _dept: Department = _
  
  private var _empId: Int = _
  def empId: Int = companion.jdoGet(this, 2)
  private def empId_=(newValue: Int) { companion.jdoSet(this, 2, newValue) }
  
  private var _name: String = _

  object Statics extends PersistenceCapable.Statics[Employee] {
    import PersistenceFlags._
  
    val jdoInheritedFieldCount: Int = 0
    val jdoFieldNames: Array[String] = Array("boss", "dept", "empId", "name")
    val jdoFieldTypes: Array[Class[_]] = Array(classOf[scalajdo.examples.Employee], classOf[scalajdo.examples.Department], classOf[Int], classOf[String])
    val jdoFieldFlags: Array[Byte] = Array(
        MEDIATE_READ + MEDIATE_WRITE,
        MEDIATE_READ + MEDIATE_WRITE,
        MEDIATE_WRITE + SERIALIZABLE,
        CHECK_READ + CHECK_WRITE + SERIALIZABLE
    ).map(_.toByte)
    val jdoIdentityType = IdentityType.APPLICATION
    val jdoPkFieldNums = Array(2)
    val jdoObjectIdClass = "Int"
    
    lazy val _accessors = Array[Employee => Any](_._boss, _._dept, _._empId, _._name)
    def accessors = _accessors
    lazy val _mutators: Array[((Employee, Any) => Unit)] = Array(
        (e: Employee, v: Any) => e._boss = v.asInstanceOf[Employee],
        (e: Employee, v: Any) => e._dept = v.asInstanceOf[Department],
        (e: Employee, v: Any) => e._empId = v.asInstanceOf[Int],
        (e: Employee, v: Any) => e._name = v.asInstanceOf[String])
    def mutators = _mutators
    val jdoPersistenceCapableSuperclass: Class[_] = null
  }
}

