package com.gu.liftweb

import scala.annotation.tailrec
import net.liftweb.json.JsonAST._
import scalaz.Functor
import JValueSyntax._


/** Represents a position within a JValue structure, comprising a value under the cursor (the focus) and a context.
  */
final case class JCursor(focus: JValue, context: JContext) {
  import JCursor._

  def replace(newFocus: JValue): JCursor =
    copy(focus = newFocus)

  def transform(f: PartialFunction[JValue, JValue]): JCursor =
    replace(focus.transform(f))

  def withFocus(f: JValue => JValue): JCursor =
    replace(f(focus))

  def withFocusF[F[_] : Functor](f: JValue => F[JValue]): F[JCursor] =
    Functor[F].map(f(focus))(replace)

  def deleteGoUp: Option[JCursor] =
    context match {
      case ArrayCtx(lefts, rights, parent) => Some(JCursor(JArray(cat(lefts, rights)), parent))
      case ObjectCtx(lefts, rights, parent) => Some(JCursor(JObject(cat(lefts, rights)), parent))
      case FieldCtx(name, ObjectCtx(lefts, rights, parent)) => Some(JCursor(JObject(cat(lefts, rights)), parent))
      case _ => None
    }

  def deleteGoRight: Option[JCursor] =
    context match {
      case c @ ArrayCtx(ls, r::rs, parent) => Some(JCursor(r, c.copy(rights = rs)))
      case _ => None
    }

  def insertLeft(newElem: JValue): Option[JCursor] =
    context match {
      case c @ ArrayCtx(ls, rs, parent) => Some(JCursor(newElem, c.copy(rights = focus::rs)))
      case _ => None
    }

  def left: Option[JCursor] =
    context match {
      case ArrayCtx(l::ls, rs, parent)  => Some(JCursor(l, ArrayCtx(ls, focus::rs, parent)))
      case _ => None
    }

  def right: Option[JCursor] =
    context match {
      case ArrayCtx(ls, r::rs, parent) => Some(JCursor(r, ArrayCtx(focus::ls, rs, parent)))
      case _ => None
    }

  def findLeft(pfn: PartialFunction[JValue, Boolean]): Option[JCursor] = {
    val p: JValue => Boolean = pfn orElse { case _ => false }
    context match {
      case ArrayCtx(ls, rs, parent)  => ls.span(l => !p(l)) match {
        case (xs, newFocus::ls) => Some(JCursor(newFocus, ArrayCtx(ls, cat(xs, rs), parent)))
        case _ => None
      }
      case _ => None
    }
  }

  def firstChild: Option[JCursor] =
    focus match {
      case JArray(x::xs) => Some(JCursor(x, ArrayCtx(Nil, xs, context)))
      case JObject(JField(name, value)::xs) => Some(JCursor(value, FieldCtx(name, ObjectCtx(Nil, xs, context))))
      case _ => None
    }

  @tailrec
  def up: Option[JCursor] =
    context match {
      case RootCtx => None
      case ArrayCtx(lefts, rights, parent) => Some(JCursor(JArray(cat(lefts, focus::rights)), parent))
      case ObjectCtx(lefts, rights, parent) => focus.toJField map (f => JCursor(JObject(cat(lefts, f::rights)), parent))
      case FieldCtx(name, parent) => JCursor(JField(name, focus), parent).up
    }

  def keySet: Option[Set[String]] =
    Some(focus) collect {
      case JObject(fields) => fields.map(_.name).toSet
    }

  def field(name: String): Option[JCursor] =
    focus match {
      case JObject(fields) => fields.span(_.name != name) match {
        case (ls, JField(n, newFocus)::rs) => Some(JCursor(newFocus, FieldCtx(n, ObjectCtx(ls, rs, context))))
        case _ => None
      }
      case _ => None
    }

  def insertChildField(name: String, value: JValue): Option[JCursor] =
    focus match {
      case JObject(fields) => Some(JCursor(value, FieldCtx(name, ObjectCtx(Nil, fields, context))))
      case _ => None
    }

  def sibling(name: String): Option[JCursor] =
    context match {
      case FieldCtx(_, _) => up flatMap (_.field(name))
      case _ => None
    }

  def insertSibling(name: String, value: JValue): Option[JCursor] =
    context match {
      case FieldCtx(_, _) => up flatMap (_.insertChildField(name, value))
      case _ => None
    }

  @tailrec
  def root: JCursor =
    up match {
      case Some(cursor) => cursor.root
      case None => this
    }

  def toJValue: JValue = root.focus

}

object JCursor {

  def fromJValue(jValue: JValue): JCursor =
    jValue match {
      case JField(name, value) => JCursor(value, FieldCtx(name, RootCtx))
      case value => JCursor(value, RootCtx)
    }

  @tailrec
  protected def cat[A](lefts: List[A], rights: List[A]): List[A] =
    lefts match {
      case Nil => rights
      case x::xs => cat(xs, x::rights)
    }

}


sealed trait JContext

case object RootCtx extends JContext

case class ArrayCtx(lefts: List[JValue], rights: List[JValue], parent: JContext) extends JContext

case class ObjectCtx(lefts: List[JField], rights: List[JField], parent: JContext) extends JContext

case class FieldCtx(name: String, parent: JContext) extends JContext

