package scalajdo

import javax.jdo.identity._
import javax.jdo.spi.StateManager
import javax.jdo.spi.JDOImplHelper
import javax.jdo.PersistenceManager
import org.datanucleus.metadata.{ IdentityType, PersistenceFlags }
import javax.jdo.spi.PersistenceCapable.{ ObjectIdFieldConsumer, ObjectIdFieldSupplier }
import java.util.BitSet
import javax.jdo.JDODetachedFieldAccessException
import javax.jdo.JDOFatalInternalException
import org.datanucleus.enhancer.spi.Persistable
import scala.reflect.runtime.universe._

trait PersistenceCapable[T <: PersistenceCapable[T]] extends javax.jdo.spi.PersistenceCapable {
  this: T =>
  import PersistenceCapable._
  
  protected val companion: Statics[T]
  
  @transient
  protected var jdoStateManager: StateManager = null
  
  @transient
  protected var jdoFlags: Byte = _
  
  protected var jdoDetachedState: Array[Object] = null
  
  protected def defer[T](f: StateManager => T, defaultVal: T): T = {
    if (jdoStateManager == null) defaultVal
    else f(jdoStateManager)
  }
    
  def jdoIsPersistent(): Boolean = defer(_.isPersistent(this), false)
  
  def jdoIsTransactional(): Boolean = defer(_.isTransactional(this), false)

  def jdoIsNew(): Boolean = defer(_.isNew(this), false)
  
  def jdoIsDeleted(): Boolean = defer(_.isDeleted(this), false)
  
  def jdoIsDetached(): Boolean = jdoStateManager == null && this.jdoDetachedState != null

  def jdoIsDirty(): Boolean = defer(_.isDirty(this), if (this.jdoIsDetached) !this.jdoDetachedState(3).asInstanceOf[BitSet].isEmpty else false)
    
  def jdoMakeDirty(fieldName: String) {
    if (this.jdoStateManager != null) this.jdoStateManager.makeDirty(this, fieldName)
    else if (this.jdoIsDetached() && fieldName != null) {
      val fldName = fieldName.substring(fieldName.lastIndexOf('.') + 1)
      companion.jdoFieldNames.zipWithIndex.foreach { case(fn, i) =>
        if (fn == fldName) {
          if (this.jdoDetachedState(2).asInstanceOf[BitSet].get(i + companion.jdoInheritedFieldCount)) {
            this.jdoDetachedState(3).asInstanceOf[BitSet].set(i + companion.jdoInheritedFieldCount)
          } else {
            throw new JDODetachedFieldAccessException("You have just attempted to acess a field/property that hasn't been detached. Please detach it first before performing this operation")
          }
        }
      }
    }
  }
  
  def jdoNewInstance(sm: StateManager): javax.jdo.spi.PersistenceCapable = {
    throw new JDOFatalInternalException("Cannot instantiate abstract class.")
  }
  
  def jdoNewInstance(sm: StateManager, objectId: Object): javax.jdo.spi.PersistenceCapable = {
    throw new JDOFatalInternalException("Cannot instantiate abstract class.")    
  }
  
  def jdoGetPersistenceManager(): PersistenceManager = {
    defer(_.getPersistenceManager(this), null)
  }
  
  def jdoGetTransactionalObjectId(): Object = defer(_.getTransactionalObjectId(this), null)
  
  def jdoGetObjectId(): Object = {
    defer(_.getObjectId(this), if (jdoIsDetached()) this.jdoDetachedState(0) else null)  
  }
  
  def jdoGetVersion(): Object = {
    defer(_.getVersion(this), if (jdoIsDetached()) this.jdoDetachedState(1) else null)
  }
  
  def jdoReplaceStateManager(sm: StateManager) {
    this.synchronized {
      if (this.jdoStateManager != null) {
        this.jdoStateManager = this.jdoStateManager.replacingStateManager(this, sm)
      } else {
        JDOImplHelper.checkAuthorizedStateManager(sm)
        this.jdoStateManager = sm
        this.jdoFlags = PersistenceFlags.LOAD_REQUIRED
      }
    }
  }
  
  def jdoReplaceFlags() {
    if (this.jdoStateManager != null)
      this.jdoFlags = this.jdoStateManager.replacingFlags(this)
  }

  def jdoReplaceFields(indices: Array[Int]) {
    if (indices == null) throw new IllegalArgumentException("argument is null");
    else indices.foreach(i => jdoReplaceField(i))
  }

  def jdoProvideFields(indices: Array[Int]) {
    if (indices == null) throw new IllegalArgumentException("argument is null")
    else indices.foreach(i => jdoProvideField(i))
  }
  
  def getAccessor(fieldNum: Int): (T => Any) = {
    if (fieldNum > 0 && fieldNum < companion.accessors.length) {
      companion.accessors(fieldNum)
    } else {
      throw new IllegalArgumentException(s"out of field index: ${fieldNum}")
    }
  }

  def getMutator(fieldNum: Int): ((T, Any) => Unit) = {
    if (fieldNum > 0 && fieldNum < companion.mutators.length) {
      companion.mutators(fieldNum)
    } else {
      throw new IllegalArgumentException(s"out of field index: ${fieldNum}")
    }
  }

  def jdoReplaceField(fieldNum: Int) {
    if (jdoStateManager == null) throw new IllegalStateException("state manager is null")
    else {
      if (fieldNum > 0 && fieldNum < companion.jdoFieldNames.length) {
        getMutator(fieldNum)(this, this.jdoStateManager.replacingObjectField(this, fieldNum))
      } else {
        throw new IllegalArgumentException(s"out of field index: ${fieldNum}") 
      }
    }
  }
  
  def jdoProvideField(fieldNum: Int) {
    if (this.jdoStateManager == null) {
      throw new IllegalStateException("state manager is null")
    } else {
      if (fieldNum > 0 && fieldNum < companion.jdoFieldNames.length) {
        this.jdoStateManager.providedObjectField(this, fieldNum, getAccessor(fieldNum)(this))
      } else {
        throw new IllegalArgumentException(s"out of field index: ${fieldNum}");
      }
    }
  }
  
  def jdoCopyField(other: T, fieldNum: Int) {
    if (fieldNum > 0 || fieldNum < companion.jdoFieldNames.length) {
      getMutator(fieldNum)(this, getAccessor(fieldNum)(other))
    } else {
      throw new IllegalArgumentException(s"out of field index: ${fieldNum}");
    }
  }
  
  def jdoCopyFields(obj: Object, fieldNums: Array[Int]) {
    val other: T = try {
      obj.asInstanceOf[T]
    } catch {
      case e: ClassCastException => throw new IllegalArgumentException(s"object is not an object of correct type")
    }
    if (this.jdoStateManager == null) throw new IllegalStateException("state manager is null")
    else if (fieldNums == null) throw new IllegalStateException("fieldNumbers is null")
    else if (this.jdoStateManager != other.asInstanceOf[T].jdoStateManager) throw new IllegalArgumentException("state managers do not match")
    else {
      fieldNums.foreach { fn => jdoCopyField(other.asInstanceOf[T], fn) }
    }
  }
  
  private def writeObject(out: java.io.ObjectOutputStream) {
    this.jdoPreSerialize()
    out.defaultWriteObject()
  }
  
  def jdoPreSerialize() {
    if (jdoStateManager != null) jdoStateManager.preSerialize(this)
  }
  
  def jdoNewObjectIdInstance(): Object = {
    if (companion.jdoIdentityType == IdentityType.DATASTORE || companion.jdoIdentityType == IdentityType.NONDURABLE) {
      null
    } else if (companion.jdoPkFieldNums.length == 1) {
      PersistenceCapable.identityForObjectId(this, this.getAccessor(companion.jdoPkFieldNums(0))(this))      
    } else {
      //TODO handle non-single-field-identity
      throw new RuntimeException("ScalaJDO doesn't handle compound identity, yet")
    }
  }

  def jdoNewObjectIdInstance(key: Object): Object = {
    if (key == null) throw new IllegalArgumentException("key is null");
    else if (!key.isInstanceOf[String]) PersistenceCapable.identityForObjectId(this, key)
    else new javax.jdo.identity.IntIdentity(getClass(), key.asInstanceOf[String])
  }

  
  def jdoCopyKeyFieldsToObjectId(oid: Object) {
    if (companion.jdoIdentityType == IdentityType.DATASTORE || companion.jdoIdentityType == IdentityType.NONDURABLE) {
      // do nothing
    } else if (companion.jdoPkFieldNums.length == 1) {
      throw new JDOFatalInternalException("It's illegal to call jdoCopyKeyFieldsToObjectId for a class with SingleFieldIdentity.")
    } else {
      //TODO handle non-single-field-identity
      throw new RuntimeException("ScalaJDO doesn't handle compound identity, yet")      
    }
  }

  def jdoCopyKeyFieldsToObjectId(fs: ObjectIdFieldSupplier, paramObject: Object) {
    if (companion.jdoIdentityType == IdentityType.DATASTORE || companion.jdoIdentityType == IdentityType.NONDURABLE) {
      // do nothing
    } else if (companion.jdoPkFieldNums.length == 1) {
      throw new JDOFatalInternalException("It's illegal to call jdoCopyKeyFieldsToObjectId for a class with SingleFieldIdentity.")
    } else {
      //TODO handle non-single-field-identity
      throw new RuntimeException("ScalaJDO doesn't handle compound identity, yet")      
    }
  }

  protected def jdoCopyKeyFieldsFromObjectId(oid: Object) {
    if (companion.jdoIdentityType == IdentityType.DATASTORE || companion.jdoIdentityType == IdentityType.NONDURABLE) {
      // do nothing
    } else if (companion.jdoPkFieldNums.length == 1) {
      if (!PersistenceCapable.idCompatible(this, oid)) {
        throw new ClassCastException("key class is not null and not compatible with right form of SingleFieldIdentity")
      } else {
        PersistenceCapable.setId(this, oid)
      }
    } else {
      //TODO handle non-single-field-identity
      throw new RuntimeException("ScalaJDO doesn't handle compound identity, yet")            
    }
  }

  protected def jdoCopyKeyFieldsFromObjectId(fc: ObjectIdFieldConsumer, oid: Object) {
    if (companion.jdoIdentityType == IdentityType.DATASTORE || companion.jdoIdentityType == IdentityType.NONDURABLE) {
      // do nothing
    } else if (companion.jdoPkFieldNums.length == 1) {
      if (fc == null) throw new IllegalArgumentException("ObjectIdFieldConsumer is null")
      else if (!PersistenceCapable.idCompatible(this, oid)) throw new ClassCastException("oid is not an instance of a compatible SingleFieldIdentity class")
      else PersistenceCapable.storeInFc(this, oid, fc)
    } else {
      //TODO handle non-single-field-identity
      throw new RuntimeException("ScalaJDO doesn't handle compound identity, yet")      
    }
  }
}

object PersistenceCapable {
  trait Statics[T <: PersistenceCapable[T]] {
    val jdoInheritedFieldCount: Int
    val jdoFieldNames: Array[String]
    val jdoFieldTypes: Array[Class[_]]
    val jdoFieldFlags: Array[Byte]
    val jdoPersistenceCapableSuperclass: Class[_]
    val jdoIdentityType: IdentityType
    val jdoObjectIdClass: String
    val jdoPkFieldNums: Array[Int]
    def accessors: Array[T => Any]
    def mutators: Array[(T, Any) => Unit]
    def jdoGetManagedFieldCount(): Int = jdoInheritedFieldCount + jdoFieldNames.length
    
    def jdoGet[Out](objPC: T, fieldNum: Int): Out = {
      if ((jdoFieldFlags(fieldNum) & PersistenceFlags.MEDIATE_READ) != 0) {
        jdoGetViaMediate[Out](objPC, fieldNum)
      } else if ((jdoFieldFlags(fieldNum) & PersistenceFlags.CHECK_READ) != 0) {
        jdoGetViaCheck[Out](objPC, fieldNum)
      } else {
        objPC.getAccessor(fieldNum)(objPC).asInstanceOf[Out]
      }
    }
    
    def jdoGetViaMediate[Out](objPC: T, fieldNum: Int): Out = {
      if (objPC.jdoStateManager != null && !objPC.jdoStateManager.isLoaded(objPC, fieldNum)) {
        objPC.jdoStateManager.getObjectField(objPC, fieldNum, objPC.getAccessor(fieldNum)(objPC)).asInstanceOf[Out]
      } else if (objPC.jdoIsDetached && 
          !objPC.jdoDetachedState(2).asInstanceOf[BitSet].get(fieldNum) && 
          !objPC.jdoDetachedState(3).asInstanceOf[BitSet].get(fieldNum)) {
        throw new JDODetachedFieldAccessException(s"You have just attempted to access field ${jdoFieldNames(fieldNum)} yet this field was not detached when you detached the object. Either dont access this field, or detach it when detaching the object.")
      } else {
        objPC.getAccessor(fieldNum)(objPC).asInstanceOf[Out]
      }
    }
    
    def jdoGetViaCheck[Out](objPC: T, fieldNum: Int): Out = {
      if (objPC.jdoFlags > 0 && objPC.jdoStateManager != null && !objPC.jdoStateManager.isLoaded(objPC, fieldNum)) {
        objPC.jdoStateManager.getObjectField(objPC, fieldNum, objPC.getAccessor(fieldNum)(objPC)).asInstanceOf[Out]
      } else if (objPC.jdoIsDetached && !objPC.jdoDetachedState(2).asInstanceOf[BitSet].get(fieldNum)) {
        throw new JDODetachedFieldAccessException(s"You have just attempted to access field ${jdoFieldNames(fieldNum)} yet this field was not detached when you detached the object. Either dont access this field, or detach it when detaching the object.")        
      } else {
        objPC.getAccessor(fieldNum)(objPC).asInstanceOf[Out]
      }
    }
    
    def jdoSet(objPC: T, fieldNum: Int, newValue: Any) {
      if ((jdoFieldFlags(fieldNum) & PersistenceFlags.MEDIATE_WRITE) != 0) {
        jdoSetViaMediate(objPC, fieldNum, newValue)
      } else if ((jdoFieldFlags(fieldNum) & PersistenceFlags.CHECK_WRITE) != 0) {
        jdoSetViaCheck(objPC, fieldNum, newValue)
      } else {
        objPC.getMutator(fieldNum)(objPC, newValue)
      }
    }
    
    def jdoSetViaMediate(objPC: T, fieldNum: Int, newValue: Any) {
      if (objPC.jdoStateManager == null) objPC.getMutator(fieldNum)(objPC, newValue)
      else objPC.jdoStateManager.setObjectField(objPC, fieldNum, objPC.getAccessor(fieldNum)(objPC), newValue)
      
      if (objPC.jdoIsDetached) objPC.jdoDetachedState(3).asInstanceOf[BitSet].set(fieldNum)
    }
    
    def jdoSetViaCheck(objPC: T, fieldNum: Int, newValue: Any) {
      if (objPC.jdoFlags != 0 && objPC.jdoStateManager != null) {
        objPC.jdoStateManager.setObjectField(objPC, fieldNum, objPC.getAccessor(fieldNum)(objPC), newValue)
      } else {
        objPC.getMutator(fieldNum)(objPC, newValue)
      }
      
      if (objPC.jdoIsDetached) objPC.jdoDetachedState(3).asInstanceOf[BitSet].set(fieldNum)
    }
  }
  
  def identityForObjectId[T <: PersistenceCapable[T]](obj: T, key: Any): SingleFieldIdentity = {
    val oidName = obj.companion.jdoObjectIdClass
    oidName match {
      case "Byte" => new ByteIdentity(obj.getClass(), key.asInstanceOf[Byte])
      case "Char" => new CharIdentity(obj.getClass(), key.asInstanceOf[Char])
      case "Int" => new IntIdentity(obj.getClass(), key.asInstanceOf[Int])
      case "Long" => new LongIdentity(obj.getClass(), key.asInstanceOf[Long])
      case "Short" => new ShortIdentity(obj.getClass(), key.asInstanceOf[Short])
      case "String" => new StringIdentity(obj.getClass(), key.asInstanceOf[String])
      case _ => new ObjectIdentity(obj.getClass(), key)
    }
  }

  def identityForObjectId[T <: PersistenceCapable[T]](obj: T, key: String): SingleFieldIdentity = {
    val oidName = obj.companion.jdoObjectIdClass
    oidName match {
      case "Byte" => new ByteIdentity(obj.getClass(), key)
      case "Char" => new CharIdentity(obj.getClass(), key)
      case "Int" => new IntIdentity(obj.getClass(), key)
      case "Long" => new LongIdentity(obj.getClass(), key)
      case "Short" => new ShortIdentity(obj.getClass(), key)
      case "String" => new StringIdentity(obj.getClass(), key)
      case _ => new ObjectIdentity(obj.getClass(), key)
    }
  }
  
  def idCompatible[T <: PersistenceCapable[T]](obj: T, oid: Object): Boolean = {
    val oidName = obj.companion.jdoObjectIdClass
    oidName match {
      case "Byte" => oid.isInstanceOf[ByteIdentity]
      case "Char" => oid.isInstanceOf[CharIdentity]
      case "Int" => oid.isInstanceOf[IntIdentity]
      case "Long" => oid.isInstanceOf[LongIdentity]
      case "Short" => oid.isInstanceOf[ShortIdentity]
      case "String" => oid.isInstanceOf[StringIdentity]
      case _ => oid.isInstanceOf[ObjectIdentity]
    }
  }

  def storeInFc[T <: PersistenceCapable[T]](obj: T, oid: Object, fc: ObjectIdFieldConsumer) = {
    val oidName = obj.companion.jdoObjectIdClass
    val fieldNum = obj.companion.jdoPkFieldNums(0)
    oidName match {
      case "Byte" => fc.storeByteField(fieldNum, oid.asInstanceOf[ByteIdentity].getKey)
      case "Char" => fc.storeCharField(fieldNum, oid.asInstanceOf[CharIdentity].getKey)
      case "Int" => fc.storeIntField(fieldNum, oid.asInstanceOf[IntIdentity].getKey)
      case "Long" => fc.storeLongField(fieldNum, oid.asInstanceOf[LongIdentity].getKey)
      case "Short" => fc.storeShortField(fieldNum, oid.asInstanceOf[ShortIdentity].getKey)
      case "String" => fc.storeStringField(fieldNum, oid.asInstanceOf[StringIdentity].getKey)
      case _ => fc.storeObjectField(fieldNum, oid.asInstanceOf[ObjectIdentity].getKey)
    }
  }
  
  def setId[T <: PersistenceCapable[T]](obj: T, oid: Object) = {
    val oidName = obj.companion.jdoObjectIdClass
    val fieldNum = obj.companion.jdoPkFieldNums(0)
    oidName match {
      case "Byte" => obj.getMutator(fieldNum)(obj, oid.asInstanceOf[ByteIdentity].getKey)
      case "Char" => obj.getMutator(fieldNum)(obj, oid.asInstanceOf[CharIdentity].getKey)
      case "Int" => obj.getMutator(fieldNum)(obj, oid.asInstanceOf[IntIdentity].getKey)
      case "Long" => obj.getMutator(fieldNum)(obj, oid.asInstanceOf[LongIdentity].getKey)
      case "Short" => obj.getMutator(fieldNum)(obj, oid.asInstanceOf[ShortIdentity].getKey)
      case "String" => obj.getMutator(fieldNum)(obj, oid.asInstanceOf[StringIdentity].getKey)
      case _ => obj.getMutator(fieldNum)(obj, oid.asInstanceOf[ObjectIdentity].getKey)
    }
  }
}

trait PersistenceCapableSubclass[T <: PersistenceCapableSubclass[T]] extends PersistenceCapable[T] {
  this: T =>
  
  override def jdoReplaceField(fieldNum: Int) {
    if (jdoStateManager == null) throw new IllegalStateException("state manager is null")
    else if (fieldNum < companion.jdoInheritedFieldCount) {
      super.jdoReplaceField(fieldNum)
    } else {
      val fNum = fieldNum - companion.jdoInheritedFieldCount
      if (fNum > 0 && fNum < companion.jdoFieldNames.length) {
        getMutator(fNum)(this, this.jdoStateManager.replacingObjectField(this, fNum))
      } else {
        throw new IllegalArgumentException(s"out of field index: ${fieldNum}") 
      }
    }
  }
  
  override def jdoProvideField(fieldNum: Int) {
    if (this.jdoStateManager == null) {
      throw new IllegalStateException("state manager is null")
    } else if (fieldNum < companion.jdoInheritedFieldCount) {
      super.jdoProvideField(fieldNum)
    } else {
      val fNum = fieldNum - companion.jdoInheritedFieldCount
      if (fNum > 0 && fNum < companion.jdoFieldNames.length) {
        this.jdoStateManager.providedObjectField(this, fNum, getAccessor(fNum)(this))
      } else {
        throw new IllegalArgumentException(s"out of field index: ${fieldNum}");
      }
    }
  }
 
  override def jdoCopyField(other: T, fieldNum: Int) {
    if (fieldNum < companion.jdoInheritedFieldCount) {
      super.jdoCopyField(other, fieldNum)
    } else {
      val fNum = fieldNum - companion.jdoInheritedFieldCount
      if (fNum > 0 && fNum < companion.jdoFieldNames.length) {
        getMutator(fieldNum)(this, getAccessor(fieldNum)(other))      
      } else {
        throw new IllegalArgumentException(s"out of field index: ${fieldNum}")
      }
    }
  }
  
  override def getAccessor(fieldNum: Int): (T => Any) = {
    if (fieldNum < companion.jdoInheritedFieldCount) {
      super.getAccessor(fieldNum)
    } else {
      val fNum = fieldNum - companion.jdoInheritedFieldCount
      if (fNum > 0 && fNum < companion.accessors.length) {
        companion.accessors(fNum)
      } else {
        throw new IllegalArgumentException(s"out of field index: ${fieldNum}")
      }
    }
  }
  
  override def getMutator(fieldNum: Int): ((T, Any) => Unit) = {
    if (fieldNum < companion.jdoInheritedFieldCount) {
      super.getMutator(fieldNum)
    } else {
      val fNum = fieldNum - companion.jdoInheritedFieldCount
      if (fNum > 0 && fNum < companion.mutators.length) {
        companion.mutators(fNum)
      } else {
        throw new IllegalArgumentException(s"out of field index: ${fieldNum}")
      }
    }
  }
}
