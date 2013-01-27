lift-json-zipper
================

A experimental zipper library for lift-json

Basic zipper API
----------------

    scala> import net.liftweb.json._, com.gu.liftweb.JValueSyntax._

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

CursorCommand API
-----------------

    scala> import scalaz.Scalaz._, com.gu.liftweb.CursorCommand._

    scala> val cmd = field("soups") >> firstChild >> insertLeft(JString("borscht"))

    scala> println(pretty(render(json execDefault cmd)))
    {
      "soups":["borscht","goulash","gumbo","minestrone"]
    }

See [CursorCommandExamples][1] for more examples.

[1]: https://github.com/bmjames/lift-json-zipper/blob/master/src/test/scala/com/gu/liftweb/CursorCommandExamples.scala
