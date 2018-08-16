package com.iz2use.indexeddb

sealed class KeyRange[K: Jsonable]
object KeyRange {
  final case class Single[K: Jsonable](value: K) extends KeyRange[K]
  final case class Bounds[K: Jsonable](lowerBound: Option[K], upperBound: Option[K], lowerBoundOpen: Boolean, upperBoundOpen: Boolean) extends KeyRange[K]
  final case class List[K: Jsonable](keys: Traversable[K]) extends KeyRange[K]
  //def apply[K: Jsonable](keys: Traversable[K]) = new List[K](keys)
  def apply[K: Jsonable](lowerBound: K, open: Boolean) = new Bounds[K](Some(lowerBound), None, open, false)
  def apply[K: Jsonable](lowerBound: K, upperBound: K) = new Bounds[K](Some(lowerBound), Some(upperBound), false, false)
  def apply[K: Jsonable](lowerBound: K, lowerOpen: Boolean, upperBound: K, upperOpen: Boolean) = new Bounds[K](Some(lowerBound), Some(upperBound), lowerOpen, upperOpen)
  def all[K: Jsonable] = new Bounds[K](None, None, false, false)
  implicit def traversableToRangeBound[K: Jsonable](keys: Traversable[K]): KeyRange[K] = new List[K](keys)
  implicit def anyToRangeBound[K: Jsonable](value: K): KeyRange[K] = Single(value)
}
