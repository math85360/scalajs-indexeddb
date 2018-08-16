package com.iz2use.indexeddb

sealed abstract class IndexTraversalDirection(val name: String)
object IndexTraversalDirection {
  case object Ascending extends IndexTraversalDirection("next")
  case object AscendingUnique extends IndexTraversalDirection("nextunique")
  case object Descending extends IndexTraversalDirection("prev")
  case object DescendingUnique extends IndexTraversalDirection("prevunique")
}