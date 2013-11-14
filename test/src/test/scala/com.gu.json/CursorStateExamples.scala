package com.gu.json

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FunSuite
import org.json4s._
import org.json4s.native.JsonMethods._

import scalaz.std.option._
import scalaz.syntax.monadPlus._

import com.gu.json.json4s._
import CursorState._
import JValueSyntax._

/** Contrived examples to demonstrate how to compose and run CursorState functions
  */
class CursorStateExamples extends FunSuite with ShouldMatchers {

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

  // Counts the number of assets and then uses that data to insert a new field, assetCount
  val addAssetCount =
    for {
      JArray(assets) <- field[JValue]("assets")
      _ <- insertFieldLeft[JValue]("assetCount", JInt(assets.length))
    } yield ()

  test("exec a cursor state function to return an updated JValue if successful") {

    json.exec(addAssetCount) should equal (Some(parse(
      """
        {
          "type":"image",
          "assetCount": 2,
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
      """)))
  }

  val deleteFirstAsset =
    field[JValue]("assets") >> head[JValue] >> deleteGoUp[JValue]

  test("execDefault returns the updated JValue if the command succeeds") {

    json.execDefault(deleteFirstAsset) should equal (parse(
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
      """))
  }

  val failingCommand = field[JValue]("assets") >> head >> left

  test("execDefault returns the initial JValue unchanged if the command fails") {

    json execDefault failingCommand should equal (json)
  }

  val moveFocus = field[JValue]("assets") >> head

  test("eval returns the final focus of the cursor if the command succeeds") {

    json eval moveFocus should equal (Some(parse(
      """
        {
          "type":"image/jpeg",
          "file":"foo.jpg"
        }
      """)))
  }

  val commandWithRetry = (field[JValue]("flibbles") orElse field("assets")) >> elem(1)

  test ("orElse combines two commands, with the second command tried if the first fails") {

    json eval commandWithRetry should equal (Some(parse(
      """
        {
          "type":"image/png",
          "file":"foo.png"
        }
      """)))
  }

  test("`having` passes if the predicate command on the r.h.s. succeeds, preserving l.h.s. state and value") {

    json.eval(field[JValue]("type") having sibling("assets")) should be (Some(JString("image")))

    json.eval(field[JValue]("type") having head) should be (None)

    json.exec { (field[JValue]("assets") having sibling("type")) >> deleteGoUp } should
      be (Some(parse("""{"type":"image"}""")))

  }

  test("`foreach` execs the supplied command on each of an array's child elements") {

    val removeFiles = field[JValue]("assets") foreach removeField("file")

    json.exec(removeFiles) should be (Some(parse("""
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

  test("`remove` syntax  on a JValue removes the element reached by a series of cursor movements") {

    json.removeAt(field[JValue]("assets") >> elem(1) >> field("file")) should be (parse("""
      {
        "type":"image",
        "assets":[
          {
            "type":"image/jpeg",
            "file":"foo.jpg"
          },
          {
            "type":"image/png"
          }
        ]
      }
    """))

  }

}

