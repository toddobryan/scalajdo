package scalajdo.converters

import scala.collection.JavaConverters._
import org.datanucleus.store.types.converters.TypeConverter

class IterableConverter[T] extends TypeConverter[Iterable[T], java.util.Collection[T]] {
  def toDatastoreType(iter: Iterable[T]): java.util.Collection[T] = iter.asJavaCollection
  def toMemberType(coll: java.util.Collection[T]): Iterable[T] = coll.asScala
}