package com.gu.json

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

import _root_.play.api.libs.json.{Json, JsValue}
import org.json4s.JValue
import org.json4s.native.JsonMethods.{compact, render}


class JsonLikeExamples extends FunSuite with ShouldMatchers {

  test("create a JSON object and render it with both json4s and play-json") {

    import J.{array, obj, string}

    def soups[J : JsonLike]: J = obj(Seq(
      "soups" -> array(Seq(
        string("goulash"), string("gumbo"), string("minestrone")
      ))
    ))

    // importing JsonLike[JValue]
    import com.gu.json.json4s._

    val json4sSoups: JValue = soups[JValue]

    // importing JsonLike[JsValue]
    import com.gu.json.play._

    val playSoups: JsValue = soups[JsValue]

    Json.stringify(playSoups) should equal (compact(render(json4sSoups)))
  }

}
