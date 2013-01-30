package com.gu.liftweb

import scala.annotation.tailrec
import scala.PartialFunction._
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
    condOpt(path) {
      case InArray(lefts, rights) :: p => JCursor(JArray(cat(lefts, rights)), p)
      case InField(name) :: InObject(lefts, rights) :: p => JCursor(JObject(cat(lefts, rights)), p)
    }

  def deleteGoRight: Option[JCursor] =
    condOpt(path) {
      case InArray(ls, r::rs) :: p => JCursor(r, InArray(ls, rs) :: p)
    }

  def insertLeft(newElem: JValue): Option[JCursor] =
    condOpt(path) {
      case InArray(ls, rs) :: p => JCursor(newElem, InArray(ls, focus::rs) :: p)
    }

  def insertRight(newElem: JValue): Option[JCursor] =
    condOpt(path) {
      case InArray(ls, rs) :: p => JCursor(newElem, InArray(focus::ls, rs) :: p)
    }

  def left: Option[JCursor] =
    condOpt(path) {
      case InArray(l::ls, rs) :: p  => JCursor(l, InArray(ls, focus::rs) :: p)
    }

  def right: Option[JCursor] =
    condOpt(path) {
      case InArray(ls, r::rs) :: p => JCursor(r, InArray(focus::ls, rs) :: p)
    }

  def findLeft(pfn: PartialFunction[JValue, Boolean]): Option[JCursor] =
    path match {
      case InArray(ls, rs) :: path  => ls.span(l => ! cond(l)(pfn)) match {
        case (xs, newFocus::ls) => Some(JCursor(newFocus, InArray(ls, cat(xs, rs)) :: path))
        case _ => None
      }
      case _ => None
    }

  def firstChild: Option[JCursor] =
    condOpt(focus) {
      case JArray(x::xs) => JCursor(x, InArray(Nil, xs) :: path)
      case JObject(JField(name, value)::xs) => JCursor(value, InField(name) :: InObject(Nil, xs) :: path)
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
    condOpt(focus) {
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
    condOpt(focus) {
      case JObject(fields) => JCursor(value, InField(name) :: InObject(Nil, fields) :: path)
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

  def rename(name: String): Option[JCursor] =
    condOpt(path) {
      case InField(_) :: p => copy(path = InField(name) :: p)
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
