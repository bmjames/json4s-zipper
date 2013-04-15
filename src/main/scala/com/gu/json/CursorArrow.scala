package com.gu.json

import scalaz._
import scalaz.Scalaz._
import scalaz.\/._

import org.json4s.JsonAST._


trait CursorArrow { self =>
  import CursorArrow._

  def run: Kleisli[CursorResult, JCursor, JCursor]

  final def compose(that: CursorArrow): CursorArrow = fromK(self.run compose that.run)
  final def andThen(that: CursorArrow): CursorArrow = that compose this

  final def <=<(that: CursorArrow): CursorArrow = compose(that)
  final def >=>(that: CursorArrow): CursorArrow = andThen(that)

  final def <*(that: CursorArrow): CursorArrow = fromK(self.run <* that.run)
  final def *>(that: CursorArrow): CursorArrow = fromK(self.run *> that.run)

  final def orElse(that: CursorArrow): CursorArrow = fromK(self.run <+> that.run)

}

object CursorArrow {

  protected type CursorResult[+A] = CursorFailure \/ A

  def apply(f: JCursor => CursorResult[JCursor]): CursorArrow =
    fromK(Kleisli(f))

  def fromK(k: Kleisli[CursorResult, JCursor, JCursor]): CursorArrow =
    new CursorArrow {
      val run = k
    }

  def withFailure(f: JCursor => Option[JCursor], msg: String): CursorArrow =
    CursorArrow { cursor =>
      f(cursor).toRightDisjunction(CursorFailure(cursor, msg))
    }

  def fail[A](at: JCursor, msg: String): CursorResult[A] = -\/(CursorFailure(at, msg))

}

/** Data type representing the position of the cursor before the failed action,
  * with a message describing the action that failed.
  */
case class CursorFailure(at: JCursor, msg: String)

object CursorArrows {
  import CursorArrow._

  def field(name: String) = withFailure(_.field(name), "field(" + name + ")")

  def firstElem = withFailure(_.firstElem, "firstElem")

  def elem(index: Int) = withFailure(_.elem(index), "elem(" + index + ")")

  def replace(newFocus: JValue) = CursorArrow(right compose (_.replace(newFocus)))

  def prepend(elem: JValue) = withFailure(_.prepend(elem), "prepend(" + elem + ")")

  def transform(pfn: PartialFunction[JValue, JValue]) = CursorArrow { cursor =>
    \/-(cursor.copy(focus = cursor.focus.transform(pfn)))
  }

  def deleteGoUp = withFailure(_.deleteGoUp, "deleteGoUp")

  def eachElem(that: CursorArrow) = CursorArrow {
    case JCursor(JArray(elems), p) =>
      for (cursors <- that.run.traverse(elems map JCursor.jCursor))
      yield JCursor(JArray(cursors map (_.toJValue)), p)
    case cursor => fail(cursor, "eachElem")
  }

}
