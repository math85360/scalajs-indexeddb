package com.iz2use.indexeddb

sealed class Range[K: Jsonable]
final case class Value[K: Jsonable](value: K) extends Range[K]
final case class RangeBounds[K: Jsonable](lowerBound: Option[K], upperBound: Option[K], lowerBoundOpen: Boolean, upperBoundOpen: Boolean) extends Range[K]
final case class KeyList[K: Jsonable](keys: Traversable[K]) extends Range[K]
object Range {
  //def apply[K: Jsonable](keys: Traversable[K]) = new KeyList[K](keys)
  def apply[K: Jsonable](lowerBound: K, open: Boolean) = new RangeBounds[K](Some(lowerBound), None, open, false)
  def apply[K: Jsonable](lowerBound: K, upperBound: K) = new RangeBounds[K](Some(lowerBound), Some(upperBound), false, false)
  def apply[K: Jsonable](lowerBound: K, lowerOpen: Boolean, upperBound: K, upperOpen: Boolean) = new RangeBounds[K](Some(lowerBound), Some(upperBound), lowerOpen, upperOpen)
  def all[K: Jsonable] = new RangeBounds[K](None, None, false, false)
  implicit def traversableToRangeBound[K: Jsonable](keys: Traversable[K]): Range[K] = new KeyList[K](keys)
  implicit def anyToRangeBound[K: Jsonable](value: K): Range[K] = Value(value)
}

sealed abstract class Direction(val name: String)
object Direction {
  case object Ascending extends Direction("next")
  case object AscendingUnique extends Direction("nextunique")
  case object Descending extends Direction("prev")
  case object DescendingUnique extends Direction("prevunique")
}