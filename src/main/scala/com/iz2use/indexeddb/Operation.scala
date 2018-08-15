package com.iz2use.indexeddb

import scalajs.js
import org.scalajs.dom.raw.{ Event, ErrorEvent, IDBCursorWithValue }

sealed trait Operation[A, B] {
  def apply(in: A)(implicit reader: js.Any => A, cursor: IDBCursorWithValue, next: B => Unit, fail: Throwable => Unit)
}
trait LowPriorityImplicitOperation {
  def readOperation[P] = new Operation[P, P] {
    override def apply(in: P)(implicit reader: js.Any => P, cursor: IDBCursorWithValue, next: P => Unit, fail: Throwable => Unit) = {
      next(in)
    }
  }
}
object Operation extends LowPriorityImplicitOperation {
  implicit def writeOperation[P](trsf: P => Unit)(implicit writer: P => js.Any) = new Operation[P, (P, P)] {
    override def apply(in: P)(implicit reader: js.Any => P, cursor: IDBCursorWithValue, next: ((P, P)) => Unit, fail: Throwable => Unit) = {
      val out = reader(cursor.value)
      trsf(out)
      val r = cursor.update(writer(out))
      r.onerror = (e: ErrorEvent) => fail(IDBUpdateException(e))
      r.onsuccess = (e: Event) => next((in, out))
    }
  }
  implicit def pfWriteOperation[P](trsf: PartialFunction[P, Unit])(implicit writer: P => js.Any) = writeOperation[P](trsf orElse PartialFunction.empty)
}