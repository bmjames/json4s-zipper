package com.gu.json

import org.json4s.JsonAST.{JInt, JString, JValue}
import scalaz.Scalaz._
import scalaz.\/

import CursorArrowSyntax.CursorArrowBuilder
import CursorState._


final class JValueOps(value: JValue) {

  def cursor: JCursor = JCursor.jCursor(value)

  def removeAt[A](command: CursorState[A]): JValue =
    execDefault(command >> deleteGoUp)

  def delete(builder: CursorArrowBuilder): JValue =
    runDefault(builder(CursorArrows.deleteGoUp))

  def mod(builder: CursorArrowBuilder)(f: JValue => JValue): JValue =
    runDefault(builder(CursorArrows.mod(f)))
    
  def modp(builder: CursorArrowBuilder)(pfn: PartialFunction[JValue, JValue]): JValue =
    runDefault(builder(CursorArrows.transform(pfn)))

  def eval[A](command: CursorState[A]): Option[A] =
    command.eval(cursor)

  def exec(command: CursorState[_]): Option[JValue] =
    command.exec(cursor) map (_.toJValue)

  def execDefault(command: CursorState[_]): JValue =
    exec(command) getOrElse value

  def run(arrow: CursorArrow): CursorFailure \/ JValue =
    arrow.run(cursor).map (_.toJValue)

  def runDefault(arrow: CursorArrow): JValue =
    run(arrow) getOrElse value

  def stringValue: Option[String] = value match {
    case JString(string) => Some(string)
    case _ => None
  }

  def bigIntValue: Option[BigInt] = value match {
    case JInt(int) => Some(int)
    case _ => None
  }

}

trait JValueSyntax {
  implicit def toJValueOps(value: JValue): JValueOps = new JValueOps(value)
}

object JValueSyntax extends JValueSyntax

