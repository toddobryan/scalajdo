package scalajdo

import javax.jdo.spi.StateManager
import javax.jdo.spi.JDOImplHelper

trait PersistenceCapable extends javax.jdo.spi.PersistenceCapable {
  @transient
  protected var jdoStateManager: StateManager = null
  
  def jdoReplaceStateManager(sm: StateManager) {
    this.synchronized {
      if (this.jdoStateManager != null) {
        this.jdoStateManager = this.jdoStateManager.replacingStateManager(this, sm);
      } else {
        JDOImplHelper.checkAuthorizedStateManager(sm);
        this.jdoStateManager = sm;
        this.jdoFlags = 1;
      }
    }
  }
}