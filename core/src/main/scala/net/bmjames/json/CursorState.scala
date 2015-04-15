package net.bmjames.json

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
object CursorState {

  protected type OptionState[S, A] = StateT[Option, S, A]

  type CursorState[J, A] = OptionState[Cursor[J], A]

  def monadState[J] = MonadState[OptionState, Cursor[J]]

  def apply[J, A](f: Cursor[J] => Option[(Cursor[J], A)]): CursorState[J, A] = StateT(f)

  def replace[J](value: J): CursorState[J, Unit] =
    CursorState(cursor => Some(cursor.replace(value), ()))

  def transform[J](pfn: PartialFunction[J, J]): CursorState[J, J] =
    returnFocus(_.transform(pfn).some)

  def field[J](name: String): CursorState[J, J] = returnFocus(_.field(name))

  def sibling[J](name: String): CursorState[J, J] = returnFocus(_.sibling(name))

  def insertChildField[J](name: String, value: J): CursorState[J, J] =
    returnFocus(_.insertField(name, value))

  def insertFieldLeft[J](name: String, value: J): CursorState[J, J] =
    returnFocus(_.insertFieldLeft(name, value))

  def insertFieldRight[J](name: String, value: J): CursorState[J, J] =
    returnFocus(_.insertFieldRight(name, value))

  def rename[J](name: String): CursorState[J, J] = returnFocus(_.rename(name))

  def insertLeft[J](elem: J): CursorState[J, J] = returnFocus(_.insertLeft(elem))

  def insertRight[J](elem: J): CursorState[J, J] = returnFocus(_.insertRight(elem))

  def left[J]: CursorState[J, J] = returnFocus(_.left)

  def leftN[J](n: Int): CursorState[J, J] =
    left[J].replicateM_(n) >> getFocus

  def right[J]: CursorState[J, J] = returnFocus(_.right)

  def rightN[J](n: Int): CursorState[J, J] =
    right[J].replicateM_(n) >> getFocus

  def findLeft[J](pfn: PartialFunction[J, Boolean]): CursorState[J, J] = returnFocus(_.findLeft(pfn))

  def head[J]: CursorState[J, J] = returnFocus(_.firstElem)

  def elem[J](n: Int): CursorState[J, J] = head[J] >> rightN(n)

  def up[J]: CursorState[J, J] = returnFocus(_.up)

  def deleteGoUp[J]: CursorState[J, J] = returnFocus(_.deleteGoUp)

  def removeField[J](name: String): CursorState[J, J] = field[J](name) >> deleteGoUp

  def root[J]: CursorState[J, J] = returnFocus(_.root.some)

  def keySet[J]: CursorState[J, Set[String]] =
    CursorState(cursor => cursor.keySet map (cursor -> _))

  /** Run the supplied function over the state value and return the resulting focus */
  def returnFocus[J](f: Cursor[J] => Option[Cursor[J]]): CursorState[J, J] =
    CursorState(f andThen (_ map (c => c -> c.focus)))

  def getFocus[J]: CursorState[J, J] = monadState.init map (_.focus)

  def orElse[J, A](a: CursorState[J, A], b: => CursorState[J, A]): CursorState[J, A] =
    CursorState(cursor => a(cursor) orElse b(cursor))

  def having[J, A, B](ca: CursorState[J, A], cb: CursorState[J, B]): CursorState[J, A] =
    for {
      a <- ca
      s <- monadState.init
      _ <- cb
      _ <- monadState.put(s)
    } yield a

  import Cursor.cursor

  def foreach[J, A](cmd: CursorState[J, A])(implicit J: JsonLike[J]): CursorState[J, J] =
    for {
      Some(children) <- getFocus[J] map (J.asArray(_))
      Some(cs) <- children.traverse(c => cmd.exec(cursor(c)).map(_.toJson)).point[({type λ[α]=CursorState[J, α]})#λ]
      newFocus = J.array(cs)
      _ <- replace(newFocus)
    } yield newFocus

  implicit class CursorStateOps[J, A](self: CursorState[J, A]) {

    def orElse(onFailure: => CursorState[J, A]): CursorState[J, A] =
      CursorState.orElse(self, onFailure)

    def having[B](guard: CursorState[J, B]): CursorState[J, A] =
      CursorState.having(self, guard)

    def foreach[B](cmd: CursorState[J, B])(implicit ev: JsonLike[J]): CursorState[J, J] =
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
  implicit def cursorStateMonadPlus[J]: MonadPlus[({type λ[α]=CursorState[J, α]})#λ] =
    new MonadPlus[({type λ[α]=CursorState[J, α]})#λ] {

      def plus[A](a: CursorState[J, A], b: => CursorState[J, A]) = orElse(a, b)

      def empty[A] = CursorState(_ => None)

      def point[A](a: => A) = CursorState(cursor => Some(cursor, a))

      def bind[A, B](fa: CursorState[J, A])(f: A => CursorState[J, B]) =
        CursorState { cursor =>
          fa(cursor) flatMap {
            case (c1, a) => f(a)(c1)
          }
        }
    }

}
