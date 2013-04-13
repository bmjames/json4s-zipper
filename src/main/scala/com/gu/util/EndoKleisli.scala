package com.gu.util

import scalaz.{ Bind, Kleisli, Monad }

/** Newtype for Kleisli[F, A, A] with a monoid in terms of composition
  */
trait EndoKleisli[F[+_], A] {

  def run: Kleisli[F, A, A]

  final def >=>(that: EndoKleisli[F, A])(implicit B: Bind[F]): EndoKleisli[F, A] =
    EndoKleisli(this.run >=> that.run)

  final def <=<(that: EndoKleisli[F, A])(implicit B: Bind[F]): EndoKleisli[F, A] =
    EndoKleisli(this.run <=< that.run)

  final def apply(a: A): F[A] =
    run.run(a)
}

object EndoKleisli {

  def apply[F[+_], A](k: Kleisli[F, A, A]): EndoKleisli[F, A] =
    new EndoKleisli[F, A] {
      val run = k
    }

  def endoKleisli[F[+_]: Monad, A](f: A => F[A]): EndoKleisli[F, A] =
    apply(Kleisli(f))

  implicit def endoKleisliMonoid[F[+_]: Monad, A] = new scalaz.Monoid[EndoKleisli[F, A]] {
    val zero = EndoKleisli(Kleisli.ask[F, A])
    def append(f1: EndoKleisli[F, A], f2: => EndoKleisli[F, A]) = f1 <=< f2
  }

}
