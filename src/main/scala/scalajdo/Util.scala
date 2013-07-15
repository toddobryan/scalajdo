package scalajdo

import org.datanucleus.metadata.PersistenceFlags._


object Util {
  def repr(s: String): String = {
    if (s == null) "null"
    else s.toList.map {
      case '\0' => "\\0"
      case '\t' => "\\t"
      case '\n' => "\\n"
      case '\r' => "\\r"
      case '\"' => "\\\""
      case '\\' => "\\\\"
      case ch if (' ' <= ch && ch <= '\u007e') => ch.toString
      case ch => {
        val hex = Integer.toHexString(ch.toInt)
        "\\u%s%s".format("0" * (4 - hex.length), hex)
      }
    }.mkString("\"", "", "\"")
  }
  
  def typeFixer(name: String): String = name match {
    case "boolean" => "Boolean"
    case "char" => "Char"
    case "byte" => "Byte"
    case "short" => "Short"
    case "int" => "Int"
    case "long" => "Long"
    case "float" => "Float"
    case "double" => "Double"
    case array if array.endsWith("[]") => s"Array[${typeFixer(array.substring(0, array.length - 2))}]"
    case javalang if javalang.startsWith("java.lang.") => javalang.substring(10)
    case _ => name
  }
  
  def persistenceFlagsToString(flags: Byte): String = flags match {
    case flags if flags == LOAD_REQUIRED => "LOAD_REQUIRED"
    case flags if flags == CHECK_WRITE => "CHECK_WRITE"
    case flags if flags == MEDIATE_WRITE => "MEDIATE_WRITE"
    case flags if flags == CHECK_READ + CHECK_WRITE => "CHECK_READ + CHECK_WRITE"
    case flags if flags == MEDIATE_READ + MEDIATE_WRITE => "MEDIATE_READ + MEDIATE_WRITE"
    case flags if flags > 16 => persistenceFlagsToString((flags - 16).toByte) + " + SERIALIZABLE"
  }
}