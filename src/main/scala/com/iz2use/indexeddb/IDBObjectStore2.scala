package com.iz2use.indexeddb

import org.scalajs.dom.raw.{ IDBIndex }
import scalajs.js

@js.native
trait IDBObjectStore2 extends js.Object {
  def createIndex(name: String, keyPath: js.Array[String],
    optionalParameters: js.Any = js.native): IDBIndex = js.native
}

