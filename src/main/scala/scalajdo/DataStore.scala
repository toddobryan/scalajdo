package scalajdo
import javax.jdo.JDOHelper

import org.datanucleus.api.jdo.{JDOPersistenceManager, JDOPersistenceManagerFactory}

object DataStore {
  private[this] lazy val pmf: JDOPersistenceManagerFactory = 
    JDOHelper.getPersistenceManagerFactory("datastore.props").asInstanceOf[JDOPersistenceManagerFactory]
  
  private[this] lazy val threadLocalPersistenceManager: ThreadLocal[ScalaPersistenceManager] =
    new ThreadLocal[ScalaPersistenceManager]()
  
  def pm: ScalaPersistenceManager = {
    if (threadLocalPersistenceManager.get() == null) {
      threadLocalPersistenceManager.set(newPm)
    }
    threadLocalPersistenceManager.get()
  }
  
  def withTransaction[A](block: (ScalaPersistenceManager => A)): A = {
    implicit val pm: ScalaPersistenceManager = DataStore.pm
    pm.beginTransaction()
    val r = block(pm)
    pm.commitTransaction()
    r
  }

  def execute[A](block: (ScalaPersistenceManager => A)): A = {
    block(pm)
  }
  
  private[DataStore] def newPm: ScalaPersistenceManager =
    new ScalaPersistenceManager(pmf.getPersistenceManager().asInstanceOf[JDOPersistenceManager])
}
