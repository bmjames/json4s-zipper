package com.gu.json

import org.json4s.JsonAST._
import scalaz.Scalaz._
import scalaz.{ Kleisli, MonadState, MonadPlus, StateT }

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
object CursorState {

  protected type OptionState[S, A] = StateT[Option, S, A]

  type CursorState[A] = OptionState[JCursor, A]

  val monadState = MonadState[OptionState, JCursor]

  def apply[A](f: JCursor => Option[(JCursor, A)]): CursorState[A] = StateT(f)

  def replace(value: JValue): CursorState[Unit] =
    CursorState(cursor => Some(cursor.replace(value), ()))

  def transform(pfn: PartialFunction[JValue, JValue]): CursorState[JValue] =
    returnFocus(_.transform(pfn).some)

  def field(name: String): CursorState[JValue] = returnFocus(_.field(name))

  def sibling(name: String): CursorState[JValue] = returnFocus(_.sibling(name))

  def insertChildField(name: String, value: JValue): CursorState[JValue] =
    returnFocus(_.insertField(name, value))

  def insertFieldLeft(name: String, value: JValue): CursorState[JValue] =
    returnFocus(_.insertFieldLeft(name, value))

  def insertFieldRight(name: String, value: JValue): CursorState[JValue] =
    returnFocus(_.insertFieldRight(name, value))

  def rename(name: String): CursorState[JValue] = returnFocus(_.rename(name))

  def insertLeft(elem: JValue): CursorState[JValue] = returnFocus(_.insertLeft(elem))

  def insertRight(elem: JValue): CursorState[JValue] = returnFocus(_.insertRight(elem))

  def left: CursorState[JValue] = returnFocus(_.left)

  def leftN(n: Int): CursorState[JValue] =
    left.replicateM_(n) >> getFocus

  def right: CursorState[JValue] = returnFocus(_.right)

  def rightN(n: Int): CursorState[JValue] =
    right.replicateM_(n) >> getFocus

  def findLeft(pfn: PartialFunction[JValue, Boolean]) = returnFocus(_.findLeft(pfn))

  def head: CursorState[JValue] = returnFocus(_.firstElem)

  def elem(n: Int): CursorState[JValue] = head >> rightN(n)

  def up: CursorState[JValue] = returnFocus(_.up)

  def deleteGoUp: CursorState[JValue] = returnFocus(_.deleteGoUp)

  def removeField(name: String): CursorState[JValue] = field(name) >> deleteGoUp

  def root: CursorState[JValue] = returnFocus(_.root.some)

  def keySet: CursorState[Set[String]] =
    CursorState(cursor => cursor.keySet map (cursor ->))

  /** Run the supplied function over the state value and return the resulting focus */
  def returnFocus(f: JCursor => Option[JCursor]): CursorState[JValue] =
    CursorState(f andThen (_ map (c => c -> c.focus)))

  def getFocus: CursorState[JValue] = monadState.init map (_.focus)

  def orElse[A](a: CursorState[A], b: => CursorState[A]): CursorState[A] =
    CursorState(cursor => a(cursor) orElse b(cursor))

  def having[A, B](ca: CursorState[A], cb: CursorState[B]): CursorState[A] =
    for {
      a <- ca
      s <- monadState.init
      _ <- cb
      _ <- monadState.put(s)
    } yield a

  import JCursor.jCursor

  def foreach[A](cmd: CursorState[A]): CursorState[JArray] =
    for {
      JArray(children) <- getFocus
      Some(cs) <- children.traverse(c => cmd.exec(jCursor(c)).map(_.toJValue)).point[CursorState]
      newFocus = JArray(cs)
      _ <- replace(newFocus)
    } yield newFocus

  implicit class CursorStateOps[A](self: CursorState[A]) {

    def orElse(onFailure: => CursorState[A]): CursorState[A] =
      CursorState.orElse(self, onFailure)

    def having[B](guard: CursorState[B]): CursorState[A] =
      CursorState.having(self, guard)

    def foreach[A](cmd: CursorState[A]): CursorState[JArray] =
      self >> CursorState.foreach(cmd)
  }

  /*
   * A MonadPlus instance is required for filtering in for-comprehensions, e.g.
   *
   *   for (JArray(results) <- field("results")) ...
   *
   * I've not thought about this too hard, but I think it probably satisfies enough laws to be reasonable.
   * See http://en.wikibooks.org/wiki/Haskell/MonadPlus#The_MonadPlus_laws
   */
  implicit val cursorStateMonadPlus: MonadPlus[CursorState] = new MonadPlus[CursorState] {

    def plus[A](a: CursorState[A], b: => CursorState[A]) = orElse(a, b)

    def empty[A] = CursorState(_ => None)

    def point[A](a: => A) = CursorState(cursor => Some(cursor, a))

    def bind[A, B](fa: CursorState[A])(f: A => CursorState[B]) =
      CursorState { cursor =>
        fa(cursor) flatMap {
          case (c1, a) => f(a)(c1)
        }
      }
  }

}
