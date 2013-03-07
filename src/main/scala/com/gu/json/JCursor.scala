package com.gu.json

import scala.annotation.tailrec
import scala.PartialFunction._
import org.json4s.JsonAST._
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
      case InArray(lefts, rights) :: p => JCursor(JArray(lefts reverse_::: rights), p)
      case InObject(_, lefts, rights) :: p => JCursor(JObject(lefts reverse_::: rights), p)
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
        case (xs, newFocus::ls) => Some(JCursor(newFocus, InArray(ls, xs reverse_::: rs) :: path))
        case _ => None
      }
      case _ => None
    }

  def firstChild: Option[JCursor] =
    condOpt(focus) {
      case JArray(x::xs) => JCursor(x, InArray(Nil, xs) :: path)
      case JObject(JField(name, value)::xs) => JCursor(value, InObject(name, Nil, xs) :: path)
    }

  def up: Option[JCursor] =
    condOpt(path) {
      case InArray(lefts, rights) :: p => JCursor(JArray(lefts reverse_::: (focus::rights)), p)
      case InObject(name, lefts, rights) :: p => JCursor(JObject(lefts reverse_::: (JField(name, focus)::rights)), p)
    }

  def keySet: Option[Set[String]] =
    condOpt(focus) {
      case JObject(fields) => fields.map(_._1).toSet
    }

  def field(name: String): Option[JCursor] =
    focus match {
      case JObject(fields) => fields.span(_._1 != name) match {
        case (ls, JField(n, newFocus)::rs) => Some(JCursor(newFocus, InObject(n, ls, rs) :: path))
        case _ => None
      }
      case _ => None
    }

  def insertChildField(name: String, value: JValue): Option[JCursor] =
    condOpt(focus) {
      case JObject(fields) => JCursor(value, InObject(name, Nil, fields) :: path)
    }

  def sibling(name: String): Option[JCursor] =
    path match {
      case InObject(_, _, _) :: _ => up flatMap (_.field(name))
      case _ => None
    }

  def insertSibling(name: String, value: JValue): Option[JCursor] =
    path match {
      case InObject(_, _, _) :: _ => up flatMap (_.insertChildField(name, value))
      case _ => None
    }

  def rename(name: String): Option[JCursor] =
    condOpt(path) {
      case InObject(_, ls, rs) :: p => copy(path = InObject(name, ls, rs) :: p)
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

  def fromJValue(jValue: JValue): JCursor = JCursor(jValue, Nil)

  type Path = List[PathElem]

  sealed trait PathElem

  case class InArray(lefts: List[JValue], rights: List[JValue]) extends PathElem

  case class InObject(fieldName: String, lefts: List[JField], rights: List[JField]) extends PathElem

}
