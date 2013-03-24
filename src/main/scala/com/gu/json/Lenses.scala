package com.gu.json

import org.json4s.JsonAST.{JString, JValue}
import scalaz._

object Lenses {

  def field(name: String): JValue @?> JValue = mkPLens(_.field(name))

  def elem(index: Int): JValue @?> JValue = mkPLens(_.elem(index))

  def strVal: JValue @?> String = PLens {
    case JString(s) => Some(Store(JString.apply, s))
    case _          => None
  }

  def mkPLens(f: JCursor => Option[JCursor]): JValue @?> JValue =
    PLens { jValue =>
      for (c <- f(JCursor.fromJValue(jValue))) yield Store(
        newFocus => c.replace(newFocus).toJValue,
        c.focus
      )
    }
}
