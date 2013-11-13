package com.gu.json

import scalaz.\/
import CursorArrowSyntax.CursorArrowBuilder


trait JValueSyntax {

  implicit class JValueOps[J](value: J) {

    def cursor(implicit ev: JsonLike[J]): Cursor[J] = Cursor.cursor(value)

    //  def removeAt[A](command: CursorState[A]): JValue =
    //    execDefault(command >> deleteGoUp)

    def delete(builder: CursorArrowBuilder[J])(implicit ev: JsonLike[J]): J =
      runDefault(builder(CursorArrows.deleteGoUp))

    def mod(builder: CursorArrowBuilder[J])(f: J => J)(implicit ev: JsonLike[J]): J =
      runDefault(builder(CursorArrows.mod(f)))

    def modp(builder: CursorArrowBuilder[J])(pfn: PartialFunction[J, J])(implicit ev: JsonLike[J]): J =
      runDefault(builder(CursorArrows.transform(pfn)))

    /*  def eval[A](command: CursorState[A]): Option[A] =
        command.eval(cursor)

      def exec(command: CursorState[_]): Option[JValue] =
        command.exec(cursor) map (_.toJson)

      def execDefault(command: CursorState[_]): JValue =
        exec(command) getOrElse value*/

    def run(arrow: CursorArrow[J])(implicit ev: JsonLike[J]): CursorFailure[J] \/ J =
      arrow.run(cursor).map (_.toJson)

    def runDefault(arrow: CursorArrow[J])(implicit ev: JsonLike[J]): J =
      run(arrow) getOrElse value

    def stringValue(implicit ev: JsonLike[J]): Option[String] =
      Lenses.strVal.get(value)

    def bigIntValue(implicit ev: JsonLike[J]): Option[BigInt] =
      Lenses.intVal.get(value)

  }

}

object JValueSyntax extends JValueSyntax
