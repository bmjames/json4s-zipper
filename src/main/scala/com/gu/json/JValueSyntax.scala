package com.gu.json

import org.json4s.JsonAST.{JInt, JString, JValue, JField}
import org.json4s.native.JsonMethods._
import scalaz.Scalaz._
import CursorCommand._


final class JValueOps(value: JValue) {

  def cursor: JCursor = JCursor.fromJValue(value)

  def eval[A](command: CursorCommand[A]): Option[A] =
    command.eval(cursor)

  def exec(command: CursorCommand[_]): Option[JValue] =
    command.exec(cursor) map (_.toJValue)

  def execDefault(command: CursorCommand[_]): JValue =
    exec(command) getOrElse value

  def stringValue: Option[String] = value match {
    case JString(string) => Some(string)
    case _ => None
  }

  def bigIntValue: Option[BigInt] = value match {
    case JInt(int) => Some(int)
    case _ => None
  }

  def prettyRender: String = pretty(render(value))

}

object JValueSyntax {
  implicit def toJValueOps(value: JValue): JValueOps = new JValueOps(value)
}
