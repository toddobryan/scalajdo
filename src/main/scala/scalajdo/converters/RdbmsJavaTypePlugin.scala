package scalajdo.converters

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

import org.datanucleus.store.types.converters.TypeConverter
import org.datanucleus.store.rdbms.mapping.java.JavaTypeMapping

trait RdbmsJavaTypePlugin[UserType, DataStoreType] {
  implicit val tag: ClassTag[UserType]
  
  def fromUserToDataStore(obj: UserType): DataStoreType
  def fromDataStoreToUser(obj: DataStoreType): UserType
   
  val typeConverter: TypeConverter[UserType, DataStoreType] = new TypeConverter[UserType, DataStoreType] {
    def toDatastoreType(obj: UserType): DataStoreType = fromUserToDataStore(obj)
    def toMemberType(obj: DataStoreType): UserType = fromDataStoreToUser(obj)
  }
  
  val typeMapping: JavaTypeMapping = new JavaTypeMapping() {
    def getJavaType(): Class[_] = implicitly[ClassTag[UserType]].runtimeClass
  }
}
  
