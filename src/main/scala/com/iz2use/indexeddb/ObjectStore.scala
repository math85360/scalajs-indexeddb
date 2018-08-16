package com.iz2use.indexeddb

import scalajs.js
import scalajs.js.JSConverters._
import scala.concurrent.{ ExecutionContext, Future, Promise }
import org.scalajs.dom
import org.scalajs.dom.raw._
import org.scalajs.dom.ext.EasySeq
import scala.util.{ Failure, Success }
import scala.collection.mutable.ArrayBuffer

abstract class ObjectStore[T, PK: Jsonable](val basename: String, val version: Int) {
  implicit val name = s"$basename-$version"
  val primaryKey: ByIndex[PK, T] = PrimaryKey[PK, T]()
  var secondaryKeys: Seq[SecondaryKey[_, T]] = Seq()
  def secondaryKey[K: Jsonable](name: String, options: KeyOptions, path: String*): SecondaryKey[K, T] = {
    val r = SecondaryKey[K, T](name, options, path)
    secondaryKeys :+= r
    r
  }
  def load[K: Jsonable](id: K, byIndex: ByIndex[K, T] = primaryKey)(implicit db: IndexedDB, ec: ExecutionContext): Future[T] = {
    implicit val _byIndex = byIndex
    db.load(id).map(read)
  }
  def deleteKey(id: PK)(implicit db: IndexedDB): Future[Unit] = db.delete(id)
  def delete(item: T)(implicit db: IndexedDB): Future[Unit] = deleteKey(keyOf(item))
  def withStore[A](f: IDBObjectStore => Future[A])(implicit db: IndexedDB): Future[A] = db.withStore(f)
  def add(item: T)(implicit db: IndexedDB): Future[_] = db.add(keyOf(item), write(item))
  def update(id: PK, transform: T => Unit)(implicit db: IndexedDB): Future[(T, T)] = {
    implicit val _read = read(_)
    implicit val _write = write(_)
    db.update(id, transform)
  }
  def put(item: T)(implicit db: IndexedDB): Future[_] = db.put(keyOf(item), write(item))
  def keyOf(item: T): PK
  def write(item: T): js.Any
  def read(item: js.Any): T
  def updateRange[K: Jsonable](byIndex: ByIndex[K, T] = primaryKey)(rng: KeyRange[K], transform: T => Unit, direction: IndexTraversalDirection = IndexTraversalDirection.Ascending)(implicit db: IndexedDB): Future[Seq[(T, T)]] = {
    implicit val _byIndex = byIndex
    implicit val _read = read(_)
    implicit val _write = write(_)
    db.range(rng, direction, transform)
  }
  def range[K: Jsonable](byIndex: ByIndex[K, T] = primaryKey)(rng: KeyRange[K], direction: IndexTraversalDirection = IndexTraversalDirection.Ascending)(implicit db: IndexedDB): Future[Seq[T]] = {
    implicit val _byIndex = byIndex
    implicit val _read = read(_)
    implicit val _write = write(_)
    db.range[K, T, T](rng, direction)
  }
  //def save(item:T)
}