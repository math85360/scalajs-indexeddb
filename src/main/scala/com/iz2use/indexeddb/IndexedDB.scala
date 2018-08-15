package com.iz2use.indexeddb

import scalajs.js
import scalajs.js.JSConverters._
import scala.concurrent.Future
import scala.concurrent.Promise
import org.scalajs.dom
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalajs.dom.raw._
import org.scalajs.dom.ext.EasySeq
import scala.util.{ Failure, Success }
import scala.collection.mutable.ArrayBuffer

trait IndexedDB {
  def objectStores: Seq[ObjectStore[_, _]]

  implicit def toJsAny[T: Jsonable](v: T): js.Any = implicitly[Jsonable[T]].serialize(v)

  def notifyUpdate(): Unit

  def version: Int

  private lazy val dbF: Future[IDBDatabase] = {
    if (js.isUndefined(dom.window.indexedDB)) {
      Future.failed(new Exception("IndexedDB not supported"))
    } else {
      dom.window.indexedDB.open("myCoachPersistence", version)
    }
  }

  def withStore[A](f: IDBObjectStore => Future[A])(implicit storeName: String): Future[A] =
    dbF flatMap { implicit db =>
      withTransaction(s"$storeName-$version") { tx =>
        val store = tx.objectStore(s"$storeName-$version")
        f(store)
      }
    }

  def withNotifiedStore[A](f: IDBObjectStore => Future[A])(implicit storeName: String): Future[A] = {
    val r = withStore[A](f)
    r onSuccess { case _ => notifyUpdate() }
    r
  }

  def withTransaction[A](storeNames: js.Any)(f: IDBTransaction => Future[A])(implicit db: IDBDatabase): Future[A] = {
    val tx = db.transaction(storeNames, "readwrite")
    tx.onabort = (e: dom.Event) => {
      dom.console.warn(e)
    }
    tx.onerror = (e: dom.ErrorEvent) => {
      dom.console.warn(e)
    }
    f(tx)
  }

  def range[K: Jsonable, P, R](rng: Range[K], direction: Direction = Direction.Ascending, op: Operation[P, R] = Operation.readOperation[P])(implicit byIndex: ByIndex[K, _], storeName: String, read: js.Any => P): Future[Seq[R]] = {
    withStore { store =>
      val kr = rng match {
        case Value(v) => IDBKeyRange only (v)
        case RangeBounds(Some(lower), Some(upper), lowerOpen, upperOpen) => IDBKeyRange bound (lower, upper, lowerOpen, upperOpen)
        case RangeBounds(Some(bound), None, open, _) => IDBKeyRange lowerBound (bound, open)
        case RangeBounds(None, Some(bound), _, open) => IDBKeyRange upperBound (bound, open)
        case KeyList(keys) => IDBKeyRange only (keys.head)
        case _ => null
      }
      val p = Promise[Seq[R]]
      val req = byIndex(store) openCursor (kr, direction.name)
      val r = ArrayBuffer.empty[R]
      implicit val fail: Throwable => Unit = p failure _
      req.onerror = (_: Event) => {
        p failure (new Exception(s"IDB Error ${req.error.name}"))
      }
      req.onsuccess = rng match {
        case KeyList(keys) =>
          val ite = keys.toIterator
          ite.next()
          (_: Event) => {
            implicit val cursor = req.result.asInstanceOf[IDBCursorWithValue]
            implicit val next: R => Unit = v => {
              r += v
              if (ite.hasNext) cursor.continue(ite.next())
              else (p success r)
            }
            if (cursor == null) {
              p success r
            } else {
              op(read(cursor.value))
            }
          }
        case _ =>
          (_: Event) => {
            implicit val cursor = req.result.asInstanceOf[IDBCursorWithValue]
            implicit val next: R => Unit = v => {
              r += v
              cursor.continue()
            }
            if (cursor == null) {
              p success r
            } else {
              op(read(cursor.value))
            }
          }
      }
      p.future
    }
  }

  def copy(source: IDBObjectStore, destination: IDBObjectStore): Future[Int] = {
    val p = Promise[Int]
    var cnt = 0
    val req = source openCursor ()
    req.onerror = (_: Event) => {
      p.failure(new Exception(s"IDB Error ${req.error.name}"))
    }
    req.onsuccess = (_: Event) => {
      val cursor = req.result.asInstanceOf[IDBCursorWithValue]
      if (cursor == null) {
        p success cnt
      } else {
        (destination put (cursor.value, cursor.key)) onComplete {
          case Success(_) =>
            cursor continue ()
            cnt += 1
          case Failure(f) =>
            p failure (new Exception(
              s"Unable to complete copy from ${source.name} to ${destination.name} at #${cnt}[=${cursor.key}]",
              f))
        }
      }
    }
    p.future
  }

  def put[K: Jsonable, P <: js.Any](id: K, value: P)(implicit storeName: String) =
    withNotifiedStore[P](_ put (value, id))

  def update[K: Jsonable, T](id: K, transform: T => Unit)(implicit storeName: String, read: js.Any => T, write: T => js.Any) =
    withNotifiedStore[(T, T)] {
      case store =>
        for {
          old <- (store get id).map(read)
          copy <- { org.scalajs.dom.console.log(old.asInstanceOf[js.Any]); (store get id).map(read) }
          r <- { org.scalajs.dom.console.log(copy.asInstanceOf[js.Any]); transform(copy); org.scalajs.dom.console.log(copy.asInstanceOf[js.Any]); (store put (write(copy), id)): Future[js.Any] }
        } yield (old, copy)
    }

  def add[K: Jsonable, P <: js.Any](id: K, value: P)(implicit storeName: String) =
    withNotifiedStore[P](_ add (value, id))

  def load[K: Jsonable, P <: js.Any](id: K)(implicit byIndex: ByIndex[K, _], storeName: String): Future[P] =
    withStore(byIndex(_) get id)

  def delete[K: Jsonable](id: K)(implicit storeName: String): Future[Unit] =
    withNotifiedStore(_ delete id)

  implicit class EasySeqDOMStringList(list: DOMStringList) extends EasySeq(list.length, list.apply)

  implicit def requestToFuture[P](req: IDBRequest): Future[P] = {
    val p = Promise[P]
    req.onsuccess = (_: Event) => {
      p success (req.result.asInstanceOf[P])
    }
    req.onerror = (_: Event) => {
      p failure (new Exception(s"IDB Error ${req.error.name}"))
    }
    p.future
  }

  implicit def databaseToFuture[P](req: IDBOpenDBRequest): Future[IDBDatabase] = {
    val p = Promise[IDBDatabase]
    var needToMigrate: Seq[(String, String)] = Seq()
    req.onsuccess = (_: Event) => {
      implicit val db = req.result.asInstanceOf[IDBDatabase]
      needToMigrate.foldLeft(Future.successful(()))({ (r, toMigrate) =>
        r andThen {
          case Success(_) =>
            withTransaction(toMigrate.productIterator.toJSArray) { tx =>
              val old = tx.objectStore(toMigrate._1)
              val res = tx.objectStore(toMigrate._2)
              copy(old, res) recoverWith {
                case f => Future.failed(new Exception(s"Cannot migrate ${toMigrate._1} to ${toMigrate._2}", f))
              } flatMap { _ =>
                requestToFuture[Unit](old.clear()) //map (_ => db deleteObjectStore old.name)
              }
            }
          case Failure(f) => f
        }
      }) onComplete {
        case Success(_) => p.success(db)
        case Failure(f) => p.failure(new Exception("Cannot migrate database", f))
      }
    }

    req.onerror = (_: Event) => {
      p.failure(new Exception(s"IDB error ${req.error.name}"))
    }
    req.onupgradeneeded = (e: IDBVersionChangeEvent) => {
      val db = req.result.asInstanceOf[IDBDatabase]
      val existentStoreNames = db.objectStoreNames
      for (objectStore <- objectStores) {
        existentStoreNames.filter(_.startsWith(s"${objectStore.basename}-")) match {
          case Nil => // Don't need to migrate anything
          case Seq(one) => // Migrate this one
            needToMigrate :+= (one -> s"${objectStore.name}-$version")
          case Seq(old, last) => // Erase old and keep last one to migrate
            //db.deleteObjectStore(old)
            needToMigrate :+= (last -> s"${objectStore.name}-$version")
          case lst =>
            // There was an error last time, throw an error
            p.failure(new Exception(lst.mkString(
              s"Cannot migrate this database from ${e.oldVersion} to ${e.newVersion}" +
                s"because ObjectStore ${objectStore.name} has all this old ObjectStore : \n",
              "\n",
              "")))
        }
        val store = db.createObjectStore(s"${objectStore.name}-$version")

        objectStore.secondaryKeys.foreach { k =>
          org.scalajs.dom.console.info(k.toString())
          k.path match {
            case Seq(one) =>
              store.createIndex(k.name, one, k.options)
            case list =>
              store.asInstanceOf[IDBObjectStore2].createIndex(k.name, list.toJSArray, k.options)
          }

        }
      }
    }
    p.future
  }
}
