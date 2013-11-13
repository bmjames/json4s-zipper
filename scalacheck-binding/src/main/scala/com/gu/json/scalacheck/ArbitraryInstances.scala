package com.gu.json.scalacheck

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import com.gu.json.JsonLike

class ArbitraryInstances[J](implicit J: JsonLike[J]) {

  implicit def json: Arbitrary[J] = Arbitrary(Gen.oneOf(obj, array, string, int, double, bool))

  // FIXME to avoid a SOE from generator recursion, arrays won't contain objects
  // FIXME also, making these lists much longer causes a SOE
  private def leaf: Gen[J] = Gen.oneOf(string, int, double, bool)
  def array: Gen[J] = Gen.listOfN(5, leaf).map(J.array(_))

  val obj: Gen[J] = Gen.listOfN(5, arbitrary[(String, J)]).map(J.obj(_))

  val string: Gen[J] = arbitrary[String].map(J.string(_))

  val int: Gen[J] = arbitrary[BigInt].map(J.int(_))

  val double: Gen[J] = arbitrary[Double].map(J.double(_))

  val bool: Gen[J] = arbitrary[Boolean].map(J.bool(_))

}

object ArbitraryInstances {
  def apply[J : JsonLike] = new ArbitraryInstances[J]
}
