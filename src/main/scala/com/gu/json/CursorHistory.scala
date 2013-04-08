package com.gu.json

import org.json4s.JsonAST.JValue

sealed trait CursorHistory

// Arrays
case class Elem(index: Int) extends CursorHistory
case object Left extends CursorHistory
case object Right extends CursorHistory
case object EachElem extends CursorHistory

/** Represents a single iteration of an EachElem */
case class Iter(history: List[CursorHistory]) extends CursorHistory

// Objects
case class Field(name: String) extends CursorHistory
case object PrevField extends CursorHistory
case object NextField extends CursorHistory

// General
case class Replace(value: JValue) extends CursorHistory
case object Up extends CursorHistory
case object DeleteGoUp extends CursorHistory
