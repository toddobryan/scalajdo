package scalajdo

import scala.collection.JavaConverters._
import org.datanucleus.query.typesafe.{BooleanExpression, 
  Expression, OrderExpression, TypesafeQuery, TypesafeSubquery}
import org.datanucleus.api.jdo.JDOPersistenceManager

class ScalaQuery[T](val query: TypesafeQuery[T]) {   
  def executeOption(): Option[T] = {
    executeList() match {
      case List(obj) => Some(obj)
      case _ => None
    }
  }
  
  def executeList(): List[T] = {
    query.executeList().asScala.toList
  }
  
  def executeResultList[R](distinct: Boolean, expr: Expression[R])(implicit man: Manifest[R]): List[R] = {
    query.executeResultList[R](man.runtimeClass.asInstanceOf[Class[R]], distinct, expr).asScala.toList
  }
  
  def executeResultUnique[R](distinct: Boolean, expr: Expression[R])(implicit man: Manifest[R]): R = {
    query.executeResultUnique[R](man.runtimeClass.asInstanceOf[Class[R]], distinct, expr)
  }
  
  def filter(expr: BooleanExpression): ScalaQuery[T] = {
    ScalaQuery[T](query.filter(expr))
  }
  
  def orderBy(orderExpr: OrderExpression[_]*): ScalaQuery[T] = {
    ScalaQuery[T](query.orderBy(orderExpr: _*))
  }
  
  def excludeSubclasses(): ScalaQuery[T] = {
    ScalaQuery[T](query.excludeSubclasses())
  }
  
  def includeSubclasses(): ScalaQuery[T] = {
    ScalaQuery[T](query.includeSubclasses())
  }
  
  def subquery[R](alias: String)(implicit man: Manifest[R]): TypesafeSubquery[R] = {
    query.subquery(man.runtimeClass.asInstanceOf[Class[R]], alias)
  }
}

object ScalaQuery {
  def apply[T](jpm: JDOPersistenceManager)(implicit man: Manifest[T]): ScalaQuery[T] = {
    new ScalaQuery[T](jpm.newTypesafeQuery[T](man.runtimeClass.asInstanceOf[Class[T]]))
  }
  
  def apply[T](query: TypesafeQuery[T]): ScalaQuery[T] = {
    new ScalaQuery[T](query)
  }
}
