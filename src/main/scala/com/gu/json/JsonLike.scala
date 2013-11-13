package com.gu.json

import scala.PartialFunction._
import org.json4s.JsonAST._

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

trait JsonLikeInstances {
  implicit val json4sJsonLike: JsonLike[JValue] = new JsonLike[JValue] {
    val nothing = JNothing
    def array(elems: Seq[JValue]) = JArray(elems.toList)
    def obj(fields: Seq[(String, JValue)]) = JObject(fields.toList)
    def string(s: String) = JString(s)
    def int(i: BigInt) = JInt(i)
    def double(d: Double) = JDouble(d)
    def bool(b: Boolean) = JBool(b)

    def asArray(j: JValue) = condOpt(j) { case JArray(elems) => elems }
    def asObj(j: JValue) = condOpt(j) { case JObject(fields) => fields }
    def asString(j: JValue) = condOpt(j) { case JString(s) => s }
    def asInt(j: JValue) = condOpt(j) { case JInt(i) => i }
    def asDouble(j: JValue) = condOpt(j) { case JDouble(d) => d }
    def asBool(j: JValue) = condOpt(j) { case JBool(b) => b }
  }
}

object JsonLike extends JsonLikeInstances
