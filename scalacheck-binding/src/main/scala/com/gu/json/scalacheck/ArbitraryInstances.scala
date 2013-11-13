package com.gu.json.scalacheck

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import com.gu.json.JsonLike

class ArbitraryInstances[J](implicit J: JsonLike[J]) {

  implicit def json: Arbitrary[J] = Arbitrary(Gen.oneOf(obj, string, int, double, bool))

  // FIXME these lists can't get much longer without causing a SOE from generator recursion
  val array: Gen[J] = Gen.listOfN(4, arbitrary[J]).map(J.array(_))
  val obj: Gen[J] = Gen.listOfN(4, arbitrary[(String, J)]).map(J.obj(_))

  val string: Gen[J] = arbitrary[String].map(J.string(_))

  val int: Gen[J] = arbitrary[BigInt].map(J.int(_))

  val double: Gen[J] = arbitrary[Double].map(J.double(_))

  val bool: Gen[J] = arbitrary[Boolean].map(J.bool(_))

}

object ArbitraryInstances {
  def apply[J : JsonLike] = new ArbitraryInstances[J]
}
