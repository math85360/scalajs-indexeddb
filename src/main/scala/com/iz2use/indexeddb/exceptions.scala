package com.iz2use.indexeddb

import org.scalajs.dom

sealed abstract class IDBException(msg: String, cause: dom.raw.ErrorEvent) extends RuntimeException(msg)
case class IDBUpdateException(cause: dom.raw.ErrorEvent) extends IDBException("Exception while updating", cause)
