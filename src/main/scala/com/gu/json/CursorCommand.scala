package com.gu.json

import org.json4s.JsonAST._
import scalaz.Scalaz._
import scalaz.{MonadState, MonadPlus, StateT}

/** Represents a transition, which may succeed or fail, from a cursor, to an updated cursor
  * together with a value: JCursor => Option[(JCursor, A)]
  *
  * Generally, movement commands return the new focus as the value, for convenience, e.g.
  *
  *   for (results <- field("results")) ...
  *
  * But you may also program entirely in imperative commands, ignoring the returned values, e.g.
  *
  *   for (_ <- field("uselessData"); _ <- deleteGoUp) yield ()
  *
  * In that case, consider instead:
  *
  *   field("uselessData") >> deleteGoUp
  *
  */
object CursorCommand {

  protected type OptionState[S, A] = StateT[Option, S, A]

  type CursorCommand[A] = OptionState[JCursor, A]

  val monadState = MonadState[OptionState, JCursor]

  def apply[A](f: JCursor => Option[(JCursor, A)]): CursorCommand[A] = StateT(f)

  def replace(value: JValue): CursorCommand[Unit] =
    CursorCommand(cursor => Some(cursor.replace(value), ()))

  def transform(pfn: PartialFunction[JValue, JValue]): CursorCommand[JValue] =
    returnFocus(_.transform(pfn))

  def field(name: String): CursorCommand[JValue] = returnFocusOpt(_.field(name))

  def sibling(name: String): CursorCommand[JValue] = returnFocusOpt(_.sibling(name))

  def insertChildField(name: String, value: JValue): CursorCommand[JValue] =
    returnFocusOpt(_.insertChildField(name, value))

  def insertSibling(name: String, value: JValue): CursorCommand[JValue] =
    returnFocusOpt(_.insertSibling(name, value))

  def rename(name: String): CursorCommand[JValue] = returnFocusOpt(_.rename(name))

  def insertLeft(elem: JValue): CursorCommand[JValue] = returnFocusOpt(_.insertLeft(elem))

  def insertRight(elem: JValue): CursorCommand[JValue] = returnFocusOpt(_.insertRight(elem))

  def left: CursorCommand[JValue] = returnFocusOpt(_.left)

  def leftN(n: Int): CursorCommand[JValue] =
    left.replicateM_(n) >> getFocus

  def right: CursorCommand[JValue] = returnFocusOpt(_.right)

  def rightN(n: Int): CursorCommand[JValue] =
    right.replicateM_(n) >> getFocus

  def findLeft(pfn: PartialFunction[JValue, Boolean]) = returnFocusOpt(_.findLeft(pfn))

  def firstChild: CursorCommand[JValue] = returnFocusOpt(_.firstChild)

  def child(n: Int): CursorCommand[JValue] = firstChild >> rightN(n)

  def up: CursorCommand[JValue] = returnFocusOpt(_.up)

  def deleteGoUp: CursorCommand[JValue] = returnFocusOpt(_.deleteGoUp)

  def root: CursorCommand[JValue] = returnFocus(_.root)

  def keySet: CursorCommand[Set[String]] =
    CursorCommand(cursor => cursor.keySet map (cursor ->))

  def returnFocus(f: JCursor => JCursor): CursorCommand[JValue] =
    returnFocusOpt(f andThen some)

  def returnFocusOpt(f: JCursor => Option[JCursor]): CursorCommand[JValue] =
    CursorCommand(f andThen (_ map extractFocus))

  def getFocus: CursorCommand[JValue] = monadState.init map (_.focus)

  def orElse[A](a: CursorCommand[A], b: => CursorCommand[A]): CursorCommand[A] =
    CursorCommand(cursor => a(cursor) orElse b(cursor))

  def having[A, B](ca: CursorCommand[A], cb: => CursorCommand[B]): CursorCommand[A] =
    for {
      a <- ca
      s <- monadState.init
      _ <- cb
      _ <- monadState.put(s)
    } yield a

  def eachChild[A](cmd: CursorCommand[A]): CursorCommand[JArray] =
    for {
      JArray(children) <- getFocus
      Some(cs) <- children.traverse(c => cmd.exec(JCursor.fromJValue(c)).map(_.toJValue)).point[CursorCommand]
      newFocus = JArray(cs)
      _ <- replace(newFocus)
    } yield newFocus

  implicit def toCursorCommandOps[A](command: CursorCommand[A]): CursorCommandOps[A] =
    new CursorCommandOps(command)

  /*
   * A MonadPlus instance is required for filtering in for-comprehensions, e.g.
   *
   *   for (JArray(results) <- field("results")) ...
   *
   * I've not thought about this too hard, but I think it probably satisfies enough laws to be reasonable.
   * See http://en.wikibooks.org/wiki/Haskell/MonadPlus#The_MonadPlus_laws
   */
  implicit val cursorCommandMonadPlus: MonadPlus[CursorCommand] = new MonadPlus[CursorCommand] {

    def plus[A](a: CursorCommand[A], b: => CursorCommand[A]) = orElse(a, b)

    def empty[A] = CursorCommand(_ => None)

    def point[A](a: => A) = CursorCommand(cursor => Some(cursor, a))

    def bind[A, B](fa: CursorCommand[A])(f: A => CursorCommand[B]) =
      CursorCommand { cursor =>
        fa(cursor) flatMap {
          case (c1, a) => f(a)(c1)
        }
      }
  }

  private def extractFocus(cursor: JCursor): (JCursor, JValue) = (cursor, cursor.focus)
}

import CursorCommand._

final class CursorCommandOps[A](self: CursorCommand[A]) {

  def orElse(onFailure: => CursorCommand[A]): CursorCommand[A] =
    CursorCommand.orElse(self, onFailure)

  def having[B](guard: => CursorCommand[B]): CursorCommand[A] =
    CursorCommand.having(self, guard)
}
