package scalajdo.converters

import org.datanucleus.store.types.converters.TypeConverter

class EnumerationConverter[Enum <: Enumeration](enum: Enum) extends TypeConverter[Enum#Value, Int] {
  def toDatastoreType(enum: Enum#Value): Int = enum.id
  def toMemberType(id: Int): Enum#Value = enum(id)
}