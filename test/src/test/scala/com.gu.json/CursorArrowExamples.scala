package com.gu.json

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FunSuite

import scalaz.\/-
import org.json4s._
import org.json4s.native.JsonMethods._
import com.gu.json.syntax._
import com.gu.json.json4s._
import CursorArrows._


class CursorArrowExamples extends FunSuite with ShouldMatchers {

  val json = parse(
    """
      {
        "type":"image",
        "assets":[
          {
            "type":"image/jpeg",
            "file":"foo.jpg",
            "caption":"Foo"
          },
          {
            "type":"image/png",
            "file":"foo.png"
          }
        ]
      }
    """)

  test("Remove a field") {

    json.runDefault { field("type") >=> deleteGoUp } should be (parse(
      """
      {
        "assets":[
          {
            "type":"image/jpeg",
            "file":"foo.jpg",
            "caption":"Foo"
          },
          {
            "type":"image/png",
            "file":"foo.png"
          }
        ]
      }
      """))

  }

  test("Replace the value of a field") {

    json.runDefault { field("type") >=> replace(JString("picture")) } should be (parse(
      """
      {
        "type":"picture",
        "assets":[
          {
            "type":"image/jpeg",
            "file":"foo.jpg",
            "caption":"Foo"
          },
          {
            "type":"image/png",
            "file":"foo.png"
          }
        ]
      }
      """))

  }

  test("Guard an action using *>") {

    // Delete any element in "assets" that has a field named "caption"
    val updated = json run field("assets") >=> eachElem(try_(field("caption") *> replace(JNothing)))

    updated.map(_.noNulls) should be (\/-(parse(
      """
      {
        "type":"image",
        "assets":[
          {
            "type":"image/png",
            "file":"foo.png"
          }
        ]
      }
      """)))

  }

  test("Guard an action using <*") {

    json.runDefault { field("assets") <* field("flibbles") >=> deleteGoUp } should be (json)

  }

  test("Retry an action with orElse") {

    val actionWithRetry: CursorArrow[JValue] = field("flibbles") orElse field("assets") >=> elem(1) >=> deleteGoUp

    json.run(actionWithRetry) should equal (\/-(parse(
      """
      {
        "type":"image",
        "assets":[
          {
            "type":"image/jpeg",
            "file":"foo.jpg",
            "caption":"Foo"
          }
        ]
      }
      """)))
  }

  test("Perform an action on each element of an array using eachElem") {

    json run field("assets") >=> eachElem(field("file") >=> deleteGoUp) should be (\/-(parse(
      """
      {
        "type":"image",
        "assets":[
          {
            "type":"image/jpeg",
            "caption":"Foo"
          },
          {
            "type":"image/png"
          }
        ]
      }
      """)))

  }

  test("Error reporting") {

    val failingAction: CursorArrow[JValue] = field("type") >=> elem(0)

    val failure: Option[CursorFailure[JValue]] = json.run(failingAction).swap.toOption

    // Reports the action that failed
    failure.map(_.msg) should be (Some("elem(0)"))

    // Reports the cursor position before failure
    failure.map(_.at.focus) should be (Some(JString("image")))
  }

  test("Symbol expression builders") {

    // modify with a PartialFunction

    json.modp("assets" \ * \ "file") { case JString(s) => JString("http://example.com/" + s) } should be (
      parse("""
        {
          "type":"image",
          "assets":[
            {
              "type":"image/jpeg",
              "file":"http://example.com/foo.jpg",
              "caption":"Foo"
            },
            {
              "type":"image/png",
              "file":"http://example.com/foo.png"
            }
          ]
        }
      """))
    
    
    // delete
    
    json.delete("assets" \ * \ "type") should be (parse("""
      {
        "type":"image",
        "assets":[
          {
            "file":"foo.jpg"
            "caption":"Foo"
          },
          {
            "file":"foo.png"
          }
        ]
      }
    """))

  }

}
