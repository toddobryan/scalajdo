package scalajdo.examples

import javax.jdo.annotations.PersistenceCapable

@PersistenceCapable
class Primitives {
  private[this] var int: Int = _
  
  private[this] var double: Double = _
  
  private[this] var boolean: Boolean = _
  
  private[this] var string: String = _
}