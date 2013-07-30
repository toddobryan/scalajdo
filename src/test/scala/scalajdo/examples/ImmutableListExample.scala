package scalajdo.examples

import scala.collection.JavaConverters._
import org.datanucleus.query.typesafe._
import org.datanucleus.api.jdo.query._

class ImmutableListExample {
  var _id: Int = _
  def id: Int = _id

  var _words: List[String] = _
  def words: List[String] = _words
  def words_=(newValue: List[String]) { this._words = newValue }
  
  def this(words: List[String]) = {
    this()
    this.words_=(words)
  }
}

trait QImmutableListExample extends PersistableExpression[ImmutableListExample] {
  private[this] lazy val _id: NumericExpression[Int] = new NumericExpressionImpl[Int](this, "_id")
  def id: NumericExpression[Int] = _id

  private[this] lazy val _words: ListExpression[java.util.List[String], String] = new ListExpressionImpl[java.util.List[String], String](this, "_words")
  def words: ListExpression[java.util.List[String], String] = _words
}

object QImmutableListExample {
  def apply(parent: PersistableExpression[ImmutableListExample], name: String, depth: Int): QImmutableListExample = {
    new PersistableExpressionImpl[ImmutableListExample](parent, name) with QImmutableListExample
  }

  def apply(cls: Class[ImmutableListExample], name: String, exprType: ExpressionType): QImmutableListExample = {
    new PersistableExpressionImpl[ImmutableListExample](cls, name, exprType) with QImmutableListExample
  }

  private[this] lazy val jdoCandidate: QImmutableListExample = candidate("this")

  def candidate(name: String): QImmutableListExample = QImmutableListExample(null, name, 5)

  def candidate(): QImmutableListExample = jdoCandidate

  def parameter(name: String): QImmutableListExample = QImmutableListExample(classOf[ImmutableListExample], name, ExpressionType.PARAMETER)

  def variable(name: String): QImmutableListExample = QImmutableListExample(classOf[ImmutableListExample], name, ExpressionType.VARIABLE)
}