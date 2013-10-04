package scalajdo

import scala.collection.JavaConverters._

import javax.jdo.JDOHelper
import org.datanucleus.api.jdo.{JDOPersistenceManager, JDOPersistenceManagerFactory}
import java.util.Properties
import org.datanucleus.store.schema.SchemaAwareStoreManager

class DataStore(val pmfGetter: () => JDOPersistenceManagerFactory) {
  private[this] var _pmf: JDOPersistenceManagerFactory = _
  
  def pmf: JDOPersistenceManagerFactory = {
    if (_pmf == null || _pmf.isClosed()) {
      _pmf = pmfGetter()
    }
    _pmf
  }
  
  def storeManager: SchemaAwareStoreManager = pmf.getNucleusContext().getStoreManager().asInstanceOf[SchemaAwareStoreManager]
  
  def persistentClasses: Set[String] = pmf.getManagedClasses().asScala.toList.map(_.getCanonicalName()).toSet

  private[this] lazy val threadLocalPersistenceManager: ThreadLocal[ScalaPersistenceManager] =
    new ThreadLocal[ScalaPersistenceManager]()
  
  def pm(): ScalaPersistenceManager = {
    if (threadLocalPersistenceManager.get() == null || threadLocalPersistenceManager.get().isClosed) {
      threadLocalPersistenceManager.set(newPm)
    }
    threadLocalPersistenceManager.get()
  }
  
  def close() {
    if (!pm.isClosed) {
      if (pm.currentTransaction.isActive) pm.commitTransaction()
      pm.close()
    }
    pmf.close()
    threadLocalPersistenceManager.set(null)
    _pmf = null
  }
  
  def withTransaction[A](block: (ScalaPersistenceManager => A)): A = {
    implicit val pm: ScalaPersistenceManager = this.pm
    try {
      pm.beginTransaction()
      val r = block(pm)
      pm.commitTransaction()
      r
    } finally {
      if (pm.currentTransaction.isActive()) pm.currentTransaction.rollback()
      pm.close()
    }
  }

  def execute[A](block: (ScalaPersistenceManager => A)): A = {
    block(pm)
  }
  
  private[DataStore] def newPm: ScalaPersistenceManager =
    new ScalaPersistenceManager(pmf.getPersistenceManager().asInstanceOf[JDOPersistenceManager])
}

//object DataStore extends DataStore(() => JDOHelper.getPersistenceManagerFactory("datastore.props").asInstanceOf[JDOPersistenceManagerFactory])
