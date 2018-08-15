package com.iz2use.indexeddb

import scalajs.js

@js.native
trait KeyOptions extends js.Any {
  var unique: Boolean = js.native
  var multiEntry: Boolean = js.native
}
object KeyOptions {
  def apply(unique: Boolean = false, multiEntry: Boolean = false): KeyOptions = {
    val r = (new js.Object).asInstanceOf[KeyOptions]
    r.unique = unique
    r.multiEntry = multiEntry
    r
  }
  val None = KeyOptions()
  val Unique = KeyOptions(true)
  val MultiEntry = KeyOptions(false, true)
  val MultiEntryUnique = KeyOptions(true, true)
}
