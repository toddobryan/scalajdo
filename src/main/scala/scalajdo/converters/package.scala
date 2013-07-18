package scalajdo

import scala.collection.{ immutable, mutable }
import scala.collection.JavaConverters.{ asScalaBufferConverter, mapAsJavaMapConverter, mapAsScalaMapConverter, mutableMapAsJavaMapConverter, seqAsJavaListConverter }

import org.datanucleus.store.rdbms.mapping.java.IntegerMapping
import org.datanucleus.store.types.converters.TypeConverter

package object converters {
  
  class EnumerationConverter[Enum <: Enumeration](enum: Enum) extends TypeConverter[Enum#Value, Int] {
    def toDatastoreType(enum: Enum#Value): Int = enum.id
    def toMemberType(id: Int): Enum#Value = enum(id)
  }
  class EnumerationMapping[Enum <: Enumeration](valClass: Class[Enum#Value]) extends IntegerMapping {
    override def getJavaType(): Class[Enum#Value] = valClass
  }

  class SeqConverter[T] extends TypeConverter[Seq[T], java.util.List[T]] {
    def toDatastoreType(seq: Seq[T]): java.util.List[T] = seq.asJava
    def toMemberType(list: java.util.List[T]): Seq[T] = list.asScala
  }

  class ImmutableMapConverter[K, V] extends TypeConverter[immutable.Map[K, V], java.util.Map[K, V]] {
    def toDatastoreType(map: immutable.Map[K, V]): java.util.Map[K, V] = map.asJava
    def toMemberType(map: java.util.Map[K, V]): immutable.Map[K, V] = map.asScala.toMap
  }

  class MutableMapConverter[K, V] extends TypeConverter[mutable.Map[K, V], java.util.Map[K, V]] {
    def toDatastoreType(map: mutable.Map[K, V]): java.util.Map[K, V] = map.asJava
    def toMemberType(map: java.util.Map[K, V]): mutable.Map[K, V] = map.asScala
  }
}