package com.gu.json

import scalaz._
import scalaz.Scalaz._
import scalaz.\/._

import org.json4s.JsonAST._


trait CursorArrow { self =>
  import CursorArrow._

  def run: Kleisli[StringDisjunction, JCursor, JCursor]

  final def compose(that: CursorArrow): CursorArrow = fromK(self.run compose that.run)
  final def andThen(that: CursorArrow): CursorArrow = that compose this

  final def <=<(that: CursorArrow): CursorArrow = compose(that)
  final def >=>(that: CursorArrow): CursorArrow = andThen(that)

  final def <*(that: CursorArrow): CursorArrow = fromK(self.run <* that.run)
  final def *>(that: CursorArrow): CursorArrow = fromK(self.run *> that.run)

  final def orElse(that: CursorArrow): CursorArrow = fromK(self.run <+> that.run)

}

object CursorArrow {

  protected type StringDisjunction[+A] = String \/ A

  def apply(f: JCursor => StringDisjunction[JCursor]): CursorArrow =
    fromK(Kleisli(f))

  def fromK(k: Kleisli[StringDisjunction, JCursor, JCursor]): CursorArrow =
    new CursorArrow {
      val run = k
    }

  def withError(f: JCursor => Option[JCursor], onError: JCursor =>  String): CursorArrow =
    CursorArrow { cursor =>
      f(cursor).toRightDisjunction(onError(cursor))
    }

}

object CursorArrows {
  import CursorArrow._

  def field(name: String) = withError(_.field(name), "No field named " + name + " in focus: " + _.focus)
  def firstElem = withError(_.firstElem, _ => "firstElem of empty JArray")
  def elem(index: Int) = withError(_.elem(index), "Array index " + index + " is out of bounds in " + _.focus)

  def replace(newFocus: JValue) = CursorArrow(right compose (_.replace(newFocus)))
  def deleteGoUp = withError(_.deleteGoUp, _ => "deleteGoUp at root of tree")

  def eachElem(that: CursorArrow) = CursorArrow {
    case JCursor(JArray(elems), p) =>
      for (cursors <- that.run.traverse(elems map JCursor.fromJValue))
      yield JCursor(JArray(cursors map (_.toJValue)), p)
    case JCursor(focus, _) => -\/("eachElem not in a JArray: " + focus)
  }

}
