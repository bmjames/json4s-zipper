package com.gu.json.scalacheck

import scalaz.syntax.apply._
import scalaz.scalacheck.ScalaCheckBinding._

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}

import com.gu.json.JsonLike

class ArbitraryInstances[J](implicit J: JsonLike[J]) {

  implicit def json: Arbitrary[J] = Arbitrary(Gen.sized(genJson))

  def genArray(size: Int): Gen[J] =
    for {
      n  <- Gen.choose(size / 3, size / 2)
      cs <- Gen.listOfN(n, genJson(n / 2))
    } yield J.array(cs)

  def genObj(size: Int): Gen[J] =
    for {
      n  <- Gen.choose(size / 3, size / 2)
      genField = arbitrary[String].tuple(genJson(n / 2))
      fs <- Gen.listOfN(n, genField)
    } yield J.obj(fs)

  def genJson(size: Int): Gen[J] =
    if (size <= 0) genLeaf else Gen.oneOf(genLeaf, genObj(size), genArray(size))

  def genLeaf: Gen[J] = Gen.oneOf(genString, genInt, genDouble, genBool)

  val genString: Gen[J] = arbitrary[String].map(J.string(_))

  val genInt: Gen[J] = arbitrary[BigInt].map(J.int(_))

  val genDouble: Gen[J] = arbitrary[Double].map(J.double(_))

  val genBool: Gen[J] = arbitrary[Boolean].map(J.bool(_))

}

object ArbitraryInstances {
  def apply[J : JsonLike] = new ArbitraryInstances[J]
}
