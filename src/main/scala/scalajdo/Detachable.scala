package scalajdo

trait Detachable[T <: PersistenceCapable[T]] extends PersistenceCapable[T] with javax.jdo.spi.Detachable {
  this: T =>
  def jdoReplaceDetachedState() {
    this.synchronized {
      if (this.jdoStateManager == null) {
        throw new IllegalStateException("state manager is null");
      } else {
        this.jdoDetachedState = this.jdoStateManager.replacingDetachedState(this, this.jdoDetachedState)
      }
    }
  }
}