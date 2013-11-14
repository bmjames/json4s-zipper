package com.gu.json

import scalaz.\/
import scalaz.std.option._
import scalaz.syntax.bind._

import CursorArrowSyntax.CursorArrowBuilder
import com.gu.json.CursorState._


trait JValueSyntax {

  implicit class JValueOps[J : JsonLike](value: J) {

    def cursor: Cursor[J] = Cursor.cursor(value)

    def removeAt[A](command: CursorState[J, A]): J =
      execDefault(command >> deleteGoUp[J])

    def delete(builder: CursorArrowBuilder[J]): J =
      runDefault(builder(CursorArrows.deleteGoUp))

    def mod(builder: CursorArrowBuilder[J])(f: J => J): J =
      runDefault(builder(CursorArrows.mod(f)))

    def modp(builder: CursorArrowBuilder[J])(pfn: PartialFunction[J, J]): J =
      runDefault(builder(CursorArrows.transform(pfn)))

    def eval[A](command: CursorState[J, A]): Option[A] =
      command.eval(cursor)

    def exec[A](command: CursorState[J, A]): Option[J] =
      command.exec(cursor) map (_.toJson)

    def execDefault[A](command: CursorState[J, A]): J =
      exec(command) getOrElse value

    def run(arrow: CursorArrow[J]): CursorFailure[J] \/ J =
      arrow.run(cursor).map (_.toJson)

    def runDefault(arrow: CursorArrow[J]): J =
      run(arrow) getOrElse value

    def stringValue: Option[String] =
      Lenses.strVal.get(value)

    def bigIntValue: Option[BigInt] =
      Lenses.intVal.get(value)

  }

}

object JValueSyntax extends JValueSyntax
