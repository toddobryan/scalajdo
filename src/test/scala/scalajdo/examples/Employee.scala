package scalajdo.examples

import javax.jdo.annotations._
import javax.jdo.spi.StateManager

@PersistenceCapable(detachable="true")
class Department {
  @PrimaryKey
  var deptId: Int = _
  
  var name: String = _
}

@PersistenceCapable(detachable="true")
class Employee {
  @PrimaryKey
  var empId: Int = _
  
  var name: String = _
  
  var boss: Employee = _
  
  @Persistent
  var dept: Department = _
}

@PersistenceCapable(detachable="true")
class Supervisor extends Employee {
  var subordinates: java.util.List[Employee] = _
}
