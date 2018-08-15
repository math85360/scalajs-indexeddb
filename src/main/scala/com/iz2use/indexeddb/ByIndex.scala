package com.iz2use.indexeddb

import org.scalajs.dom.raw.{ IDBIndex, IDBObjectStore }

sealed abstract class ByIndex[K: Jsonable, T] {
  def apply(store: IDBObjectStore): IDBIndex
}
final case class PrimaryKey[K: Jsonable, T]() extends ByIndex[K, T] {
  override def apply(store: IDBObjectStore) = store.asInstanceOf[IDBIndex]
}
final case class SecondaryKey[K: Jsonable, T](name: String, options: KeyOptions, path: Seq[String]) extends ByIndex[K, T] {
  override def apply(store: IDBObjectStore) = store.index(name)
}
