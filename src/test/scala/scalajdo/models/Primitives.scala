package scalajdo.models

class Primitives {
  private[this] var boolean: Boolean = _
  
  private[this] var char: Char = _
  
  private[this] var float: Float = _
  
  private[this] var double: Double = _
  
  private[this] var byte: Byte = _
  
  private[this] var short: Short = _
  
  private[this] var int: Int = _
  
  private[this] var long: Long = _
  
  def this(boolean: Boolean, char: Char, float: Float, double: Double, byte: Byte, short: Short, int: Int, long: Long) = {
    this()
    this.boolean = boolean
    this.char = char
    this.float = float
    this.double = double
    this.byte = byte
    this.short = short
    this.int = int
    this.long = long
  }
}