package net.bmjames.json

import scalaz._
import scalaz.Scalaz._
import scalaz.\/._


trait CursorArrow[J] { self =>

  import CursorArrow._

  type Result[A] = CursorResult[J, A]

  def run: Kleisli[Result, Cursor[J], Cursor[J]]

  final def compose(that: CursorArrow[J]): CursorArrow[J] = fromK(self.run compose that.run)
  final def andThen(that: CursorArrow[J]): CursorArrow[J] = that compose this

  final def <=<(that: CursorArrow[J]): CursorArrow[J] = compose(that)
  final def >=>(that: CursorArrow[J]): CursorArrow[J] = andThen(that)

  final def <*(that: CursorArrow[J]): CursorArrow[J] = fromK(self.run <* that.run)
  final def *>(that: CursorArrow[J]): CursorArrow[J] = fromK(self.run *> that.run)

  final def orElse(that: CursorArrow[J]): CursorArrow[J] = fromK(self.run <+> that.run)

}

object CursorArrow {

  type CursorResult[J, A] = CursorFailure[J] \/ A

  def apply[J](f: Cursor[J] => CursorResult[J, Cursor[J]]): CursorArrow[J] =
    fromK[J](Kleisli.kleisli[({type λ[α]=CursorResult[J, α]})#λ, Cursor[J], Cursor[J]](f))

  def fromK[J](k: Kleisli[({type λ[α]=CursorResult[J, α]})#λ, Cursor[J], Cursor[J]]): CursorArrow[J] =
    new CursorArrow[J] {
      val run = k
    }

  def withFailure[J](f: Cursor[J] => Option[Cursor[J]], msg: String): CursorArrow[J] =
    CursorArrow[J] { cursor =>
      f(cursor).toRightDisjunction(CursorFailure(cursor, msg))
    }

  def fail[J, A](at: Cursor[J], msg: String): CursorResult[J, A] = -\/(CursorFailure(at, msg))

}

/** Data type representing the position of the cursor before the failed action,
  * with a message describing the action that failed.
  */
case class CursorFailure[J](at: Cursor[J], msg: String)

object CursorArrows {
  import CursorArrow._

  def field[J](name: String): CursorArrow[J] = withFailure(_.field(name), "field(" + name + ")")

  def firstElem[J]: CursorArrow[J] = withFailure(_.firstElem, "firstElem")

  def elem[J](index: Int): CursorArrow[J] = withFailure(_.elem(index), "elem(" + index + ")")

  def replace[J](newFocus: J) = CursorArrow[J](right compose (_.replace(newFocus)))

  def prepend[J](elem: J): CursorArrow[J] = withFailure(_.prepend(elem), "prepend(" + elem + ")")

  def insertField[J](name: String, value: J): CursorArrow[J] =
    withFailure(_.insertField(name, value), s"insertField($value)")

  def mod[J](f: J => J) = CursorArrow[J] { cursor => \/-(cursor.withFocus(f)) }
  
  def transform[J](pfn: PartialFunction[J, J]) = CursorArrow[J] { cursor => \/-(cursor.transform(pfn)) }

  def deleteGoUp[J]: CursorArrow[J] = withFailure(_.deleteGoUp, "deleteGoUp")

  def eachElem[J](that: CursorArrow[J])(implicit J: JsonLike[J]) = CursorArrow[J] { case cursor @ Cursor(focus, p) =>
    J.asArray(focus) match {
      case Some(elems) =>
        for (cursors <- that.run.traverse(elems map Cursor.cursor[J]))
        yield Cursor(J.array(cursors map (_.toJson)), p)
      case None => fail(cursor, "eachElem")
    }
  }

  def try_[J](arr: CursorArrow[J]): CursorArrow[J] =
    CursorArrow[J] { cursor => arr.run(cursor) orElse cursor.pure[({type λ[α]=CursorResult[J, α]})#λ] }

}


trait CursorArrowSyntax {
  
  import CursorArrows._

  type CursorArrowBuilder[J] = CursorArrow[J] => CursorArrow[J]

  implicit class BuilderOps[J : JsonLike](self: CursorArrowBuilder[J]) {
    def \(that: String): CursorArrowBuilder[J] = arr => self(field(that) >=> arr)
    def \(that: CursorArrowBuilder[J]): CursorArrowBuilder[J] = arr => self(that(arr))
  }

  implicit class CursorStateExpr[J : JsonLike](self: String) {
    def \(that: String): CursorArrowBuilder[J] = field(self) >=> field(that) >=> _
    def \(that: CursorArrowBuilder[J]): CursorArrowBuilder[J] = arr => field(self) >=> that(arr)
  }

  def * [J : JsonLike] = new CursorArrowBuilder[J] {
    def apply(v1: CursorArrow[J]) = eachElem(v1)
  }

}

object CursorArrowSyntax extends CursorArrowSyntax
