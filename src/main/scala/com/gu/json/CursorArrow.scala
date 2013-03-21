package com.gu.json

import scalaz._
import scalaz.Scalaz._
import org.json4s.JsonAST.JArray
import org.json4s.JsonAST.JArray


trait CursorArrow { self =>

  protected type StringDisjunction[+A] = String \/ A

  def run: Kleisli[StringDisjunction, JCursor, JCursor]

  final def compose(that: CursorArrow): CursorArrow = new CursorArrow {
    val run = self.run compose that.run
  }
  final def andThen(that: CursorArrow): CursorArrow = that compose this

  final def <=<(that: CursorArrow): CursorArrow = compose(that)
  final def >=>(that: CursorArrow): CursorArrow = andThen(that)

  final def <*(that: CursorArrow): CursorArrow = new CursorArrow {
    val run = self.run <* that.run
  }
  final def *>(that: CursorArrow): CursorArrow = new CursorArrow {
    val run = self.run *> that.run
  }

  final def orElse(that: CursorArrow): CursorArrow = new CursorArrow {
    val run = self.run <+> that.run
  }

}

object CursorArrow {

  def apply(f: JCursor => Option[JCursor], onError: JCursor =>  String): CursorArrow =
    new CursorArrow {
      val run = Kleisli[StringDisjunction, JCursor, JCursor](cursor => f(cursor).toRightDisjunction(onError(cursor)))
    }

}

object CursorArrows {

  def field(name: String) = CursorArrow(_.field(name), "No field named " + name + " in focus: " + _.focus)

  def firstElem = CursorArrow(_.firstElem, _ => "firstElem of empty JArray")

  def elem(index: Int) = CursorArrow(_.elem(index), "Array index " + index + " is out of bounds in " + _.focus)

  def deleteGoUp = CursorArrow(_.deleteGoUp, _ => "deleteGoUp at root of tree")

  def eachElem(that: CursorArrow): CursorArrow = new CursorArrow {
    val run = Kleisli[StringDisjunction, JCursor, JCursor] {
      case JCursor(JArray(elems), p) =>
        for {
          cursors <- that.run.traverse(elems map JCursor.fromJValue)
          newElems = cursors map (_.toJValue)
        } yield JCursor(JArray(newElems), p)
      case JCursor(focus, _) => -\/("eachElem not in a JArray: " + focus)
    }
  }

}
