package com.gu.json

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.json4s.JsonAST.JString
import com.gu.json.JCursor.InArray

class JCursorTest extends FunSuite with ShouldMatchers {

  test("findLeft") {
    
    val cursor = JCursor(
      JString("qux"),
      InArray(List(JString("baz"), JString("bar"), JString("foo")), Nil) :: Nil
    )
    
    cursor.findLeft { case JString(s) => s startsWith "b" } should be (Some(
      JCursor(JString("baz"), InArray(List(JString("bar"), JString("foo")), List(JString("qux"))) :: Nil)
    ))
    
  }
  
    test("findRight") {
    
    val cursor = JCursor(
      JString("qux"),
      InArray(Nil, List(JString("foo"), JString("bar"), JString("baz"))) :: Nil
    )
    
    cursor.findRight { case JString(s) => s startsWith "b" } should be (Some(
      JCursor(JString("bar"), InArray(List(JString("foo"), JString("qux")), List(JString("baz"))) :: Nil)
    ))

  }
  
}
