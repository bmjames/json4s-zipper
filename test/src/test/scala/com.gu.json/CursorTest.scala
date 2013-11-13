package com.gu.json

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

import org.json4s.JsonAST._
import com.gu.json.Cursor.InArray
import com.gu.json.json4s._

class CursorTest extends FunSuite with ShouldMatchers {

  test("findLeft") {
    
    val cursor = Cursor[JValue](
      JString("qux"),
      InArray(List(JString("baz"), JString("bar"), JString("foo")), Nil) :: Nil
    )
    
    cursor.findLeft { case JString(s) => s startsWith "b" } should be (Some(
      Cursor[JValue](JString("baz"), InArray(List(JString("bar"), JString("foo")), List(JString("qux"))) :: Nil)
    ))
    
  }
  
    test("findRight") {
    
    val cursor = Cursor[JValue](
      JString("qux"),
      InArray(Nil, List(JString("foo"), JString("bar"), JString("baz"))) :: Nil
    )
    
    cursor.findRight { case JString(s) => s startsWith "b" } should be (Some(
      Cursor[JValue](JString("bar"), InArray(List(JString("foo"), JString("qux")), List(JString("baz"))) :: Nil)
    ))

  }
  
}
