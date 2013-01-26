package com.gu.liftweb

import scala.annotation.tailrec
import net.liftweb.json.JsonAST._
import scalaz.Functor
import JValueSyntax._
import JCursor._


/** Represents a position within a JValue structure, comprising a value under the cursor (the focus) and a context.
  */
final case class JCursor(focus: JValue, path: Path) {

  def replace(newFocus: JValue): JCursor =
    copy(focus = newFocus)

  def transform(f: PartialFunction[JValue, JValue]): JCursor =
    replace(focus.transform(f))

  def withFocus(f: JValue => JValue): JCursor =
    replace(f(focus))

  def withFocusF[F[_] : Functor](f: JValue => F[JValue]): F[JCursor] =
    Functor[F].map(f(focus))(replace)

  def deleteGoUp: Option[JCursor] =
    path match {
      case InArray(lefts, rights) :: p => Some(JCursor(JArray(cat(lefts, rights)), p))
      case InObject(lefts, rights) :: p => Some(JCursor(JObject(cat(lefts, rights)), p))
      case InField(name) :: InObject(lefts, rights) :: p => Some(JCursor(JObject(cat(lefts, rights)), p))
      case _ => None
    }

  def deleteGoRight: Option[JCursor] =
    path match {
      case InArray(ls, r::rs) :: p => Some(JCursor(r, InArray(ls, rs) :: p))
      case _ => None
    }

  def insertLeft(newElem: JValue): Option[JCursor] =
    path match {
      case InArray(ls, rs) :: p => Some(JCursor(newElem, InArray(ls, focus::rs) :: p))
      case _ => None
    }

  def left: Option[JCursor] =
    path match {
      case InArray(l::ls, rs) :: p  => Some(JCursor(l, InArray(ls, focus::rs) :: p))
      case _ => None
    }

  def right: Option[JCursor] =
    path match {
      case InArray(ls, r::rs) :: p => Some(JCursor(r, InArray(focus::ls, rs) :: p))
      case _ => None
    }

  def findLeft(pfn: PartialFunction[JValue, Boolean]): Option[JCursor] = {
    val p: JValue => Boolean = pfn orElse { case _ => false }
    path match {
      case InArray(ls, rs) :: path  => ls.span(l => !p(l)) match {
        case (xs, newFocus::ls) => Some(JCursor(newFocus, InArray(ls, cat(xs, rs)) :: path))
        case _ => None
      }
      case _ => None
    }
  }

  def firstChild: Option[JCursor] =
    focus match {
      case JArray(x::xs) => Some(JCursor(x, InArray(Nil, xs) :: path))
      case JObject(JField(name, value)::xs) => Some(JCursor(value, InField(name) :: InObject(Nil, xs) :: path))
      case _ => None
    }

  @tailrec
  def up: Option[JCursor] =
    path match {
      case Nil => None
      case InArray(lefts, rights) :: p => Some(JCursor(JArray(cat(lefts, focus::rights)), p))
      case InObject(lefts, rights) :: p => focus.toJField map (f => JCursor(JObject(cat(lefts, f::rights)), p))
      case InField(name) :: p => JCursor(JField(name, focus), p).up
    }

  def keySet: Option[Set[String]] =
    Some(focus) collect {
      case JObject(fields) => fields.map(_.name).toSet
    }

  def field(name: String): Option[JCursor] =
    focus match {
      case JObject(fields) => fields.span(_.name != name) match {
        case (ls, JField(n, newFocus)::rs) => Some(JCursor(newFocus, InField(n) :: InObject(ls, rs) :: path))
        case _ => None
      }
      case _ => None
    }

  def insertChildField(name: String, value: JValue): Option[JCursor] =
    focus match {
      case JObject(fields) => Some(JCursor(value, InField(name) :: InObject(Nil, fields) :: path))
      case _ => None
    }

  def sibling(name: String): Option[JCursor] =
    path match {
      case InField(_) :: _ => up flatMap (_.field(name))
      case _ => None
    }

  def insertSibling(name: String, value: JValue): Option[JCursor] =
    path match {
      case InField(_) :: _ => up flatMap (_.insertChildField(name, value))
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
      case JField(name, value) => JCursor(value, InField(name) :: Nil)
      case value => JCursor(value, Nil)
    }

  @tailrec
  protected def cat[A](lefts: List[A], rights: List[A]): List[A] =
    lefts match {
      case Nil => rights
      case x::xs => cat(xs, x::rights)
    }

  type Path = List[PathElem]

  sealed trait PathElem

  case class InArray(lefts: List[JValue], rights: List[JValue]) extends PathElem

  case class InObject(lefts: List[JField], rights: List[JField]) extends PathElem

  case class InField(name: String) extends PathElem

}
