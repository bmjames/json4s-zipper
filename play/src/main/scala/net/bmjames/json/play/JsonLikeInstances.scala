package net.bmjames.json.play

import scala.PartialFunction._
import _root_.play.api.libs.json._
import net.bmjames.json.JsonLike

trait JsonLikeInstances {

  implicit val playJsonLike: JsonLike[JsValue] = new JsonLike[JsValue] {
    def array(elems: Seq[JsValue]) = JsArray(elems)
    def obj(fields: Seq[(String, JsValue)]) = JsObject(fields)
    def string(s: String) = JsString(s)
    def int(i: BigInt) = JsNumber(BigDecimal(new java.math.BigDecimal(i.bigInteger)))
    def double(d: Double) = JsNumber(BigDecimal(d))
    def bool(b: Boolean) = JsBoolean(b)

    def asArray(j: JsValue) = condOpt(j) { case JsArray(elems) => elems.toList }
    def asObj(j: JsValue) = condOpt(j) { case JsObject(fields) => fields.toList }
    def asString(j: JsValue) = condOpt(j) { case JsString(s) => s }
    def asInt(j: JsValue) = condOpt(j) { case JsNumber(i) if i.isWhole => i.toBigInt }
    def asDouble(j: JsValue) = condOpt(j) { case JsNumber(d) => d.toDouble }
    def asBool(j: JsValue) = condOpt(j) { case JsBoolean(b) => b }
  }

}

object JsonLikeInstances extends JsonLikeInstances
