package scalajdo.mapping

import scala.collection.immutable.Iterable
import scala.reflect.ClassTag
import org.datanucleus.store.rdbms.mapping.java.AbstractContainerMapping

class IterableMapping[T <: Iterable[_]](implicit tag: ClassTag[T]) extends AbstractContainerMapping {
  def getJavaType(): Class[_] = implicitly[ClassTag[T]].runtimeClass
}

class ListMapping extends IterableMapping[List[_]]