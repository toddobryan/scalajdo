package scalajdo

import java.io.File
import scala.collection.JavaConverters._
import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfterAll
import scalajdo.examples.{ ImmutableListExample, QImmutableListExample }
import org.datanucleus.store.schema.SchemaAwareStoreManager
import java.util.Properties
import org.datanucleus.api.jdo.JDOPersistenceManagerFactory
import javax.jdo.JDOHelper

class ImmutableListExampleTest extends FunSuite with BeforeAndAfterAll {
  object DataStore extends DataStore(() => JDOHelper.getPersistenceManagerFactory("scalajdo.examples").asInstanceOf[JDOPersistenceManagerFactory])
  
  override def beforeAll() {
    val classes = DataStore.persistentClasses.asJava
    val props = new Properties()
    DataStore.storeManager.deleteSchema(classes, props)
    DataStore.storeManager.createSchema(classes, props)
  }
  
  override def afterAll() {
    DataStore.close()
  }
  
  test("putting objects in db") {
    DataStore.withTransaction { pm =>
      val l1 = new ImmutableListExample(List("once", "upon", "a", "time"))
      val l2 = new ImmutableListExample(List("call", "me", "Ishmael"))
      pm.makePersistent(l1)
      pm.makePersistent(l2)
    }
  }
  
  test("retrieving objects from db") {
    DataStore.withTransaction { pm =>
      val cand = QImmutableListExample.candidate
      val mobyDick = pm.query[ImmutableListExample].filter(cand.words.contains("Ishmael")).executeOption()
      assert(mobyDick.isDefined)
      assert(mobyDick.get.words === List("call", "me", "Ishmael"))
      val fairyTale = pm.query[ImmutableListExample].filter(cand.words.size().eq(4)).executeOption()
      assert(fairyTale.isDefined)
      assert(fairyTale.get.words === List("once", "upon", "a", "time"))
    }
  }
}