package scalajdo

import scala.collection.JavaConverters._
import javax.jdo.{Extent, Query}
import org.datanucleus.api.jdo.JDOPersistenceManager
import org.datanucleus.api.jdo.JDOPersistenceManagerFactory

class ScalaPersistenceManager(val jpm: JDOPersistenceManager) {
  def beginTransaction() {
    jpm.currentTransaction.begin()
  }
  
  def commitTransaction() {
    try {
      jpm.currentTransaction.commit()
    } finally {
      if (jpm.currentTransaction.isActive) {
        jpm.currentTransaction.rollback()
      }
    }
  }

  def commitTransactionAndClose() {
    try {
      jpm.currentTransaction.commit()
    } finally {
      if (jpm.currentTransaction.isActive) {
        jpm.currentTransaction.rollback()
      }
      jpm.close()
    }
  }
  
  def currentTransaction(): javax.jdo.Transaction = {
    jpm.currentTransaction()
  }
  
  def isClosed(): Boolean = jpm.isClosed()
  
  def close() {
    jpm.close()
  }
  
  def deletePersistent[T](dataObj: T) {
    jpm.deletePersistent(dataObj)
  }
  
  def deletePersistentAll[T](dataObjs: Seq[T]) {
    jpm.deletePersistentAll(dataObjs.asJava)
  }
  
  def evictAll() {
    jpm.evictAll()
  }
  
  def makePersistent[T](dataObj: T): T = { // TODO: can this be PersistenceCapable
    jpm.makePersistent[T](dataObj)
  }
  
  def makePersistentAll[T](dataObjs: Seq[T]): Seq[T] = {
    jpm.makePersistentAll[T](dataObjs.asJava).asScala.toList
  }
  
  def extent[T](includeSubclasses: Boolean = true)(implicit man: Manifest[T]): Extent[T] = {
    jpm.getExtent[T](man.runtimeClass.asInstanceOf[Class[T]], includeSubclasses)
  }
  
  def newQuery[T](extent: Extent[T]): Query = jpm.newQuery(extent)
  
  def query[T](implicit man: Manifest[T]): ScalaQuery[T] = ScalaQuery[T](jpm)
  
  def detachCopy[T](obj: T)(implicit man: Manifest[T]): T = jpm.detachCopy(obj)
}

object ScalaPersistenceManager {
  def create(pmf: JDOPersistenceManagerFactory): ScalaPersistenceManager = {
    val spm = new ScalaPersistenceManager(pmf.getPersistenceManager().asInstanceOf[JDOPersistenceManager])
    spm.beginTransaction()
    spm
  }
}
