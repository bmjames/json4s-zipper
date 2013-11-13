package com.gu.json

import scala.annotation.tailrec
import scala.PartialFunction._

import scalaz.{Endomorphic, EphemeralStream, Functor}
import scalaz.std.option._
import scalaz.syntax.monoid._

import Endomorphic._
import Cursor._


/** Represents a position within a JSON structure, comprising a value under the cursor (the focus) and a context.
  */
case class Cursor[J](focus: J, path: Path[J])(implicit J: JsonLike[J]) {

  /** Replace the value at the focus */
  def replace(newFocus: J): Cursor[J] =
    copy(focus = newFocus)

  /** Transform the value at the focus */
  def transform(f: PartialFunction[J, J]): Cursor[J] =
    replace(f.lift(focus).getOrElse(focus))

  /** Transform the value at the focus */
  def withFocus(f: J => J): Cursor[J] =
    replace(f(focus))

  def withFocusF[F[_] : Functor](f: J => F[J]): F[Cursor[J]] =
    Functor[F].map(f(focus))(replace)

  /** Replace the value at the focus with JNothing */
  def setNothing: Cursor[J] =
    replace(J.nothing)

  /** Delete the value at the focus, and move up one level */
  def deleteGoUp: Option[Cursor[J]] =
    condOpt(path) {
      case InArray(lefts, rights) :: p => Cursor(J.array(lefts reverse_::: rights), p)
      case InObject(_, lefts, rights) :: p => Cursor(J.obj(lefts reverse_::: rights), p)
    }

  /** Delete the array element at the focus, and move to the next element on the right */
  def deleteGoRight: Option[Cursor[J]] =
    condOpt(path) {
      case InArray(ls, r::rs) :: p => Cursor(r, InArray(ls, rs) :: p)
    }

  /** Insert a new array element at the left of the focus, and move focus to the new element */
  def insertLeft(newElem: J): Option[Cursor[J]] =
    condOpt(path) {
      case InArray(ls, rs) :: p => Cursor(newElem, InArray(ls, focus::rs) :: p)
    }

  /** Insert a new array element at the right of the focus, and move focus to the new element */
  def insertRight(newElem: J): Option[Cursor[J]] =
    condOpt(path) {
      case InArray(ls, rs) :: p => Cursor(newElem, InArray(focus::ls, rs) :: p)
    }

  /** Prepend an element to the array at the focus of the cursor */
  def prepend(elem: J): Option[Cursor[J]] =
    J.asArray(focus) map (elems => copy(focus = J.array(elem :: elems)))

  /** Move the focus left by one array element */
  def left: Option[Cursor[J]] =
    condOpt(path) {
      case InArray(l::ls, rs) :: p  => Cursor(l, InArray(ls, focus::rs) :: p)
    }

  /** Move the focus left n times in an array */
  def leftN(n: Int): Option[Cursor[J]] =
    endoKleisli[Option, Cursor[J]](_.left).multiply(n).run(this)

  /** Move the focus right by one array element */
  def right: Option[Cursor[J]] =
    condOpt(path) {
      case InArray(ls, r::rs) :: p => Cursor(r, InArray(focus::ls, rs) :: p)
    }

  /** Move the focus right n times in an array */
  def rightN(n: Int): Option[Cursor[J]] =
    endoKleisli[Option, Cursor[J]](_.right).multiply(n).run(this)

  import EphemeralStream._

  /** Stream of cursors resulting from moving left in an array */
  final def lefts: EphemeralStream[Cursor[J]] =
    left.fold(emptyEphemeralStream[Cursor[J]])(cursor => cons(cursor, cursor.lefts))

  /** Stream of cursors resulting from moving right in an array */
  final def rights: EphemeralStream[Cursor[J]] =
    right.fold(emptyEphemeralStream[Cursor[J]])(cursor => cons(cursor, cursor.rights))

  /** Find an array element to the left of the focus matching a predicate */
  def findLeft(pfn: PartialFunction[J, Boolean]): Option[Cursor[J]] =
    lefts.find { case Cursor(f, _) => cond(f)(pfn) }

  /** Find an array element to the right of the focus matching a predicate */
  def findRight(pfn: PartialFunction[J, Boolean]): Option[Cursor[J]] =
    rights.find { case Cursor(f, _) => cond(f)(pfn) }

  /** Move the focus down to the first element of an array */
  def firstElem: Option[Cursor[J]] =
    condOpt(J.asArray(focus)) {
      case Some(x::xs) => Cursor(x, InArray(Nil, xs) :: path)
    }

  /** Move the focus down to the array element at the specified index */
  def elem(index: Int): Option[Cursor[J]] =
    firstElem flatMap (_.rightN(index))

  /** Move the focus up one level */
  def up: Option[Cursor[J]] =
    condOpt(path) {
      case InArray(lefts, rights) :: p => Cursor(J.array(lefts reverse_::: (focus::rights)), p)
      case InObject(name, lefts, rights) :: p => Cursor(J.obj(lefts reverse_::: ((name, focus)::rights)), p)
    }

  def keySet: Option[Set[String]] =
    J.asObj(focus) map (_.map(_._1).toSet)

  /** Move the focus to the named field in an object */
  def field(name: String): Option[Cursor[J]] =
    J.asObj(focus) flatMap { fields =>
      condOpt(fields.span(_._1 != name)) {
        case (ls, (n, newFocus)::rs) => Cursor(newFocus, InObject(n, ls, rs) :: path)
      }
    }

  /** Prepend a field to the object in the focus, and move the focus to the value of the new field */
  def insertField(name: String, value: J): Option[Cursor[J]] =
    J.asObj(focus) map (fields => Cursor(value, InObject(name, Nil, fields) :: path))

  /** Move the focus to the named field, at the same level as the current focus in an object */
  def sibling(name: String): Option[Cursor[J]] =
    path match {
      case InObject(_, _, _) :: _ => up flatMap (_.field(name))
      case _ => None
    }

  /** Insert a new field at the left of the focus, and move focus to the new field value */
  def insertFieldLeft(name: String, value: J): Option[Cursor[J]] =
    condOpt(path) {
      case InObject(n, ls, rs) :: p => Cursor(value, InObject(name, ls, (n, focus) :: rs) :: p)
    }

  /** Insert a new field at the right of the focus, and move focus to the new field value */
  def insertFieldRight(name: String, value: J): Option[Cursor[J]] =
    condOpt(path) {
      case InObject(n, ls, rs) :: p => Cursor(value, InObject(name, (n, focus) :: ls, rs) :: p)
    }

  /** Rename the field at the focus */
  def rename(name: String): Option[Cursor[J]] =
    condOpt(path) {
      case InObject(_, ls, rs) :: p => copy(path = InObject(name, ls, rs) :: p)
    }

  /** Go back to the root of the tree */
  @tailrec
  final def root: Cursor[J] =
    up match {
      case Some(cursor) => cursor.root
      case None => this
    }

  /** Retrieve the value at the root */
  def toJson: J = root.focus

}

object Cursor {
  
  def cursor[J : JsonLike](json: J): Cursor[J] = Cursor(json, Nil)

  type Path[J] = List[PathElem[J]]

  type Field[J] = (String, J)
  
  sealed trait PathElem[+J]

  case class InArray[J](lefts: List[J], rights: List[J]) extends PathElem[J]

  case class InObject[J](fieldName: String, lefts: List[Field[J]], rights: List[Field[J]]) extends PathElem[J]

}
