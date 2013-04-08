package com.gu.json

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FunSuite
import org.json4s._
import org.json4s.native.JsonMethods._

import scalaz._

import CursorArrows._
import JValueSyntax._
import scalaz.\/-
import com.gu.json.CursorFailure


class CursorArrowExamples extends FunSuite with ShouldMatchers {

  val json = parse(
    """
      {
        "type":"image",
        "assets":[
          {
            "type":"image/jpeg",
            "file":"foo.jpg"
          },
          {
            "type":"image/png",
            "file":"foo.png"
          }
        ]
      }
    """)

  test("Remove a field") {

    json runDefault field("type") >=> deleteGoUp should be (parse(
      """
      {
        "assets":[
          {
            "type":"image/jpeg",
            "file":"foo.jpg"
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

    json runDefault field("type") >=> replace(JString("picture")) should be (parse(
      """
      {
        "type":"picture",
        "assets":[
          {
            "type":"image/jpeg",
            "file":"foo.jpg"
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

    json run field("assets") >=> firstElem >=> field("file") *> deleteGoUp should be (\/-(parse(
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

    json runDefault field("assets") <* field("flibbles") >=> deleteGoUp should be (json)

  }

  test("Retry an action with orElse") {

    val actionWithRetry = field("flibbles") orElse field("assets") >=> elem(1) >=> deleteGoUp

    json run actionWithRetry should equal (\/-(parse(
      """
      {
        "type":"image",
        "assets":[
          {
            "type":"image/jpeg",
            "file":"foo.jpg"
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
            "type":"image/jpeg"
          },
          {
            "type":"image/png"
          }
        ]
      }
      """)))

  }

  test("Error reporting") {

    val failingAction = field("type") >=> elem(0)

    val failure: Option[CursorFailure] = json.run(failingAction).swap.toOption

    // Reports the action that failed
    failure.map(_.msg) should be (Some("elem(0)"))

    // Reports the cursor position before failure
    failure.map(_.at.focus) should be (Some(JString("image")))
  }

}
