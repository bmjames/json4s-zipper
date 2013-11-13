package com.gu.json

/** Typeclass for a data structure representing an AST for JSON */
trait JsonLike[J] {

  def nothing: J
  def array(elems: Seq[J]): J
  def obj(fields: Seq[(String, J)]): J
  def string(s: String): J
  def int(s: BigInt): J
  def double(d: Double): J
  def bool(b: Boolean): J

  def asArray(j: J): Option[List[J]]
  def asObj(j: J): Option[List[(String, J)]]
  def asString(j: J): Option[String]
  def asInt(j: J): Option[BigInt]
  def asDouble(j: J): Option[Double]
  def asBool(j: J): Option[Boolean]

}
