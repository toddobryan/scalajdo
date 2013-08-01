package scalajdo

import java.io.File

import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfterAll
import scalajdo.examples.{ ImmutableListExample, QImmutableListExample }

class ImmutableListExampleTest extends FunSuite with BeforeAndAfterAll {
  override def beforeAll() {
    val db: File = new File("data.h2.db")
    if (db.exists) db.delete()
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