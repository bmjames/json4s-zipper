package com.gu.json

import org.json4s.JsonAST.JValue

sealed trait CursorMovement

// Arrays
case class Elem(index: Int) extends CursorMovement
case object Left extends CursorMovement
case object Right extends CursorMovement
case object EachElem extends CursorMovement

// Objects
case class Field(name: String) extends CursorMovement
case object PrevField extends CursorMovement
case object NextField extends CursorMovement

// General
case class Replace(value: JValue) extends CursorMovement
case object Up extends CursorMovement
case object DeleteGoUp extends CursorMovement
