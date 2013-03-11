json4s-zipper
=============

A experimental zipper library for the [json4s][1] AST.

Basic zipper API
----------------

    scala> import org.json4s.native.JsonMethods._, com.gu.json.JValueSyntax._

    scala> val json = parse("""
         |   {
         |     "soups":["goulash","gumbo","minestrone"]
         |   }
         | """)

    scala> val updatedSoups = for {
         |   a <- json.cursor.field("soups")
         |   b <- a.firstChild
         |   c <- b.insertLeft(JString("borscht"))
         | } yield c

    scala> for (cursor <- updatedSoups) println(pretty(render(cursor.toJValue)))
    {
      "soups":["borscht","goulash","gumbo","minestrone"]
    }

CursorState API
-----------------

    scala> import scalaz.Scalaz._, com.gu.json.CursorState._, org.json4s.JsonAST._

    scala> val cmd = field("soups") >> firstChild >> insertLeft(JString("borscht"))

    scala> println(pretty(render(json execDefault cmd)))
    {
      "soups":["borscht","goulash","gumbo","minestrone"]
    }

See [CursorStateExamples][2] for more examples.

[1]: http://json4s.org/
[2]: https://github.com/bmjames/json4s-zipper/blob/master/src/test/scala/com/gu/json/CursorStateExamples.scala
