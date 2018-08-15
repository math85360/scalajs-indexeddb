package com.iz2use.indexeddb

import scalajs.js

trait Jsonable[T] {
  def serialize(t: T): js.Any
}
trait LowPriorityJsonable {
  implicit object AnyJsonable extends Jsonable[js.Any] {
    override def serialize(t: js.Any) = t
  }
  implicit object StringJsonable extends Jsonable[String] {
    override def serialize(t: String) = t
  }
  implicit def tuple2Jsonable[A1, A2](implicit evA1: Jsonable[A1], evA2: Jsonable[A2]) = new Jsonable[(A1, A2)] {
    override def serialize(t: (A1, A2)) = js.Array(evA1.serialize(t._1), evA2.serialize(t._2))
  }
  implicit def tuple3Jsonable[A1, A2, A3](implicit evA1: Jsonable[A1], evA2: Jsonable[A2], evA3: Jsonable[A3]) = new Jsonable[(A1, A2, A3)] {
    override def serialize(t: (A1, A2, A3)) = js.Array(evA1.serialize(t._1), evA2.serialize(t._2), evA3.serialize(t._3))
  }
  /*implicit def optionJsonable[A1](implicit ev: Jsonable[A1]) = new Jsonable[Option[A1]] {
    override def serialize(t: Option[A1]) = t match {
      case None    => js.undefined
      case Some(t) => ev.serialize(t)
    }
  }*/
}
object Jsonable extends LowPriorityJsonable {
  implicit object LongJsonable extends Jsonable[Long] {
    override def serialize(t: Long) = t
  }
  implicit object DoubleJsonable extends Jsonable[Double] {
    override def serialize(t: Double) = t
  }
  /*implicit object DateJsonable extends Jsonable[js.Date] {
    override def serialize(t: js.Date) = t
  }*/ /*
  implicit def optionJsonable[T: Jsonable]: Jsonable[Option[T]] = new Jsonable[Option[T]] {
    override def serialize(t: Option[T]): js.Any =
      t match {
        case Some(t) => implicitly[Jsonable[T]].serialize(t)
        case None    => js.undefined
      }
  }*/
}