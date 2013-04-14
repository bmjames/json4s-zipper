package com.gu.json

import scala.annotation.tailrec
import scala.PartialFunction._
import org.json4s.JsonAST._
import scalaz._, Scalaz._

import com.gu.util.EndoKleisli._
import JValueSyntax._
import JCursor._

/** Represents a position within a JValue structure, comprising a value under the cursor (the focus) and a context.
  */
final case class JCursor(focus: JValue, path: Path) {

  /** Replace the value at the focus */
  def replace(newFocus: JValue): JCursor =
    copy(focus = newFocus)

  /** Transform the value at the focus */
  def transform(f: PartialFunction[JValue, JValue]): JCursor =
    replace(focus.transform(f))

  /** Transform the value at the focus */
  def withFocus(f: JValue => JValue): JCursor =
    replace(f(focus))

  def withFocusF[F[_] : Functor](f: JValue => F[JValue]): F[JCursor] =
    Functor[F].map(f(focus))(replace)

  /** Delete the value at the focus, and move up one level */
  def deleteGoUp: Option[JCursor] =
    condOpt(path) {
      case InArray(lefts, rights) :: p => JCursor(JArray(lefts reverse_::: rights), p)
      case InObject(_, lefts, rights) :: p => JCursor(JObject(lefts reverse_::: rights), p)
    }

  /** Delete the array element at the focus, and move to the next element on the right */
  def deleteGoRight: Option[JCursor] =
    condOpt(path) {
      case InArray(ls, r::rs) :: p => JCursor(r, InArray(ls, rs) :: p)
    }

  /** Insert a new array element at the left of the focus, and move focus to the new element */
  def insertLeft(newElem: JValue): Option[JCursor] =
    condOpt(path) {
      case InArray(ls, rs) :: p => JCursor(newElem, InArray(ls, focus::rs) :: p)
    }

  /** Insert a new array element at the right of the focus, and move focus to the new element */
  def insertRight(newElem: JValue): Option[JCursor] =
    condOpt(path) {
      case InArray(ls, rs) :: p => JCursor(newElem, InArray(focus::ls, rs) :: p)
    }

  /** Prepend an element to the array at the focus of the cursor */
  def prepend(elem: JValue): Option[JCursor] =
    condOpt(focus) {
      case JArray(elems) => copy(focus = JArray(elem :: elems))
    }

  /** Move the focus left by one array element */
  def left: Option[JCursor] =
    condOpt(path) {
      case InArray(l::ls, rs) :: p  => JCursor(l, InArray(ls, focus::rs) :: p)
    }

  /** Move the focus left n times in an array */
  def leftN(n: Int): Option[JCursor] =
    endoKleisli[Option, JCursor](_.left).multiply(n)(this)

  /** Move the focus right by one array element */
  def right: Option[JCursor] =
    condOpt(path) {
      case InArray(ls, r::rs) :: p => JCursor(r, InArray(focus::ls, rs) :: p)
    }

  /** Move the focus right n times in an array */
  def rightN(n: Int): Option[JCursor] =
    endoKleisli[Option, JCursor](_.right).multiply(n)(this)

  /** Find an array element to the left of the focus matching */
  def findLeft(pfn: PartialFunction[JValue, Boolean]): Option[JCursor] =
    path match {
      case InArray(ls, rs) :: path  => ls.span(l => ! cond(l)(pfn)) match {
        case (xs, newFocus::ls) => Some(JCursor(newFocus, InArray(ls, xs reverse_::: rs) :: path))
        case _ => None
      }
      case _ => None
    }

  /** Move the focus down to the first element of an array */
  def firstElem: Option[JCursor] =
    condOpt(focus) {
      case JArray(x::xs) => JCursor(x, InArray(Nil, xs) :: path)
    }

  /** Move the focus down to the array element at the specified index */
  def elem(index: Int): Option[JCursor] =
    firstElem flatMap (_.rightN(index))

  /** Move the focus up one level */
  def up: Option[JCursor] =
    condOpt(path) {
      case InArray(lefts, rights) :: p => JCursor(JArray(lefts reverse_::: (focus::rights)), p)
      case InObject(name, lefts, rights) :: p => JCursor(JObject(lefts reverse_::: (JField(name, focus)::rights)), p)
    }

  def keySet: Option[Set[String]] =
    condOpt(focus) {
      case JObject(fields) => fields.map(_._1).toSet
    }

  /** Move the focus to the named field in an object */
  def field(name: String): Option[JCursor] =
    focus match {
      case JObject(fields) => fields.span(_._1 != name) match {
        case (ls, JField(n, newFocus)::rs) => Some(JCursor(newFocus, InObject(n, ls, rs) :: path))
        case _ => None
      }
      case _ => None
    }

  /** Prepend a field to the object in the focus, and move the focus to the value of the new field */
  def insertField(name: String, value: JValue): Option[JCursor] =
    condOpt(focus) {
      case JObject(fields) => JCursor(value, InObject(name, Nil, fields) :: path)
    }

  /** Move the focus to the named field, at the same level as the current focus in an object */
  def sibling(name: String): Option[JCursor] =
    path match {
      case InObject(_, _, _) :: _ => up flatMap (_.field(name))
      case _ => None
    }

  /** Insert a new field at the left of the focus, and move focus to the new field value */
  def insertFieldLeft(name: String, value: JValue): Option[JCursor] =
    condOpt(path) {
      case InObject(n, ls, rs) :: p => JCursor(value, InObject(name, ls, JField(n, focus) :: rs) :: p)
    }

  /** Insert a new field at the right of the focus, and move focus to the new field value */
  def insertFieldRight(name: String, value: JValue): Option[JCursor] =
    condOpt(path) {
      case InObject(n, ls, rs) :: p => JCursor(value, InObject(name, JField(n, focus) :: ls, rs) :: p)
    }

  /** Rename the field at the focus */
  def rename(name: String): Option[JCursor] =
    condOpt(path) {
      case InObject(_, ls, rs) :: p => copy(path = InObject(name, ls, rs) :: p)
    }

  /** Go back to the root of the tree */
  @tailrec
  def root: JCursor =
    up match {
      case Some(cursor) => cursor.root
      case None => this
    }

  /** Retrieve the JValue at the root */
  def toJValue: JValue = root.focus

}

object JCursor {

  def jCursor(jValue: JValue): JCursor = JCursor(jValue, Nil)

  type Path = List[PathElem]

  sealed trait PathElem

  case class InArray(lefts: List[JValue], rights: List[JValue]) extends PathElem

  case class InObject(fieldName: String, lefts: List[JField], rights: List[JField]) extends PathElem

}
