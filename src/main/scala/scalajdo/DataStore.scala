package scalajdo
import javax.jdo.JDOHelper
import org.datanucleus.api.jdo.{JDOPersistenceManager, JDOPersistenceManagerFactory}
import java.util.Properties

object DataStore {
  private[this] var _pmf: JDOPersistenceManagerFactory = _
  
  def pmf: JDOPersistenceManagerFactory = {
    if (_pmf == null || _pmf.isClosed()) {
      _pmf = JDOHelper.getPersistenceManagerFactory("datastore.props").asInstanceOf[JDOPersistenceManagerFactory]
    }
    _pmf
  }
  
  private[this] lazy val threadLocalPersistenceManager: ThreadLocal[ScalaPersistenceManager] =
    new ThreadLocal[ScalaPersistenceManager]()
  
  def pm(): ScalaPersistenceManager = {
    if (threadLocalPersistenceManager.get() == null || threadLocalPersistenceManager.get().isClosed) {
      threadLocalPersistenceManager.set(newPm)
    }
    threadLocalPersistenceManager.get()
  }
  
  def close() {
    pmf.close()
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
