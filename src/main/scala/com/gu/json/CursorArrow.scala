package com.gu.json

import scalaz._
import scalaz.Scalaz._

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

  final def orElse(that: CursorArrow): CursorArrow = CursorArrow { c => run(c) orElse that.run(c) }

  private [json] def mapHistory(f: List[CursorHistory] => List[CursorHistory]): CursorArrow =
    CursorArrow { cursor =>
      EitherT[CursorHistoryWriter, Failed, JCursor](run(cursor).run.mapWritten(f))
    }

}

object CursorArrow {

  protected type CursorHistoryWriter[+A] = Writer[List[CursorHistory], A]
  protected type CursorResult[+A] = EitherT[CursorHistoryWriter, Failed, A]

  def apply(f: JCursor => CursorResult[JCursor]): CursorArrow =
    fromK(Kleisli(f))

  def fromK(k: Kleisli[CursorResult, JCursor, JCursor]): CursorArrow =
    new CursorArrow {
      val run = k
    }

  def withFailure(f: JCursor => Option[JCursor], movement: CursorHistory): CursorArrow =
    CursorArrow { cursor =>
      EitherT[CursorHistoryWriter, Failed, JCursor](f(cursor).toRightDisjunction(Failed(cursor.focus)).set(List(movement)))
    }

  def fail[A](at: JValue, movement: CursorHistory): CursorResult[A] =
    EitherT[CursorHistoryWriter, Failed, A](-\/(Failed(at)).set(List(movement)))

}

private [json] case class Failed(cursor: JValue)

object CursorArrows {
  import CursorArrow._

  def field(name: String) = withFailure(_.field(name), Field(name))

  def firstElem = withFailure(_.firstElem, Elem(0))

  def elem(index: Int) = withFailure(_.elem(index), Elem(index))

  def replace(newFocus: JValue) = withFailure(_.replace(newFocus).some, Replace(newFocus))

  //def prepend(elem: JValue) = withFailure(_.prepend(elem), "prepend(" + elem + ")")

  def deleteGoUp = withFailure(_.deleteGoUp, DeleteGoUp)

  def eachElem(that: CursorArrow) = CursorArrow {
    case JCursor(JArray(elems), p) =>
      for {
        cursors <- that
          .mapHistory(h => List(Iter(h))).run // Put history for each iteration into an Iter()
          .traverse(elems map (e => JCursor.fromJValue(e)))
      }
      yield JCursor(JArray(cursors map (_.toJValue)), p)
    case cursor => fail(cursor.focus, EachElem)
  }

}
