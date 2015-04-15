package net.bmjames.json.json4s

import scala.PartialFunction._
import org.json4s._
import net.bmjames.json.JsonLike

trait JsonLikeInstances {

  implicit val json4sJsonLike: JsonLike[JValue] = new JsonLike[JValue] {
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

object JsonLikeInstances extends JsonLikeInstances
