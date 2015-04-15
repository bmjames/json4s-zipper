json4s-zipper
=============

This is an experimental [zipper][1] library for the [json4s][2] and [Play JSON][3] ASTs. It is designed to be easy to make compatible with
the other JSON libraries, by implementing the `JsonLike` typeclass.

The goals of this library are twofold:
  * To implement purely functional modifications to immutable JSON structures;
  * To support writing functions that are reusable with various JSON libraries, including ones not yet in existence.

## How to get it

To use with json4s (3.2.x):

    libraryDependencies += "net.bmjames" %% "json-zipper-core" % "0.2"

    libraryDependencies += "net.bmjames" %% "json-zipper-json4s" % "0.2"

To use with Play (2.3.x):

    libraryDependencies += "net.bmjames" %% "json-zipper-core" % "0.2"

    libraryDependencies += "net.bmjames" %% "json-zipper-play" % "0.2"

## Examples

To start with, here is some JSON, parsed into the `JValue` AST from json4s. (You can start a REPL in which to follow
these examples by running `sbt "project test" test:console` from the root of this source tree.)

    import org.json4s.native.JsonMethods._
    
    val json = parse("""{"soups":["goulash","gumbo","minestrone"]}""")

### Cursor API (quite stable)

This is the core zipper data type upon which the other APIs are based. It's a little verbose, but you can use this
API directly. Most operations result in an `Option[Cursor[_]]`, as they may fail (e.g. if you use `field`, but the
cursor is not currently on an object).

    import net.bmjames.json.json4s._
    import net.bmjames.json.syntax._
    import org.json4s.JString

    val cursor = json.cursor // A cursor focusing on the root of the JSON object

    val updatedCursor = for {
      a <- cursor.field("soups")         // Go to field "soups"
      b <- a.prepend(JString("borscht")) // Prepend to the array
    } yield b

    for (c <- updatedCursor) println(compact(render(c.toJson)))
    // {"soups":["borscht","goulash","gumbo","minestrone"]}

### XPath-style syntax (quite experimental)

This library supports modification of a JSON structure using an xpath-like syntax.

    // Append the string " is tasty!" to each string in the array within the field "soups"
    val tastySoups = json.mod ("soups" \ *) { case JString(s) => JString(s + " is tasty!") }
    
    println(compact(render(tastySoups)))
    // {"soups":["goulash is tasty!","gumbo is tasty!","minestrone is tasty!"]}

The XPath-like syntax is implemented using the `CursorArrow` API. See
[CursorArrowExamples](test/src/test/scala/com/gu/json/CursorArrowExamples.scala) for examples of using `CursorArrow`
directly.

### Lenses (quite stable)

Lenses enable bidirectional transformations on data structures; i.e. the ability to query and update a *view* of the
structure, with modifications propagating back as changes to the original structure.

This library implements Scalaz partial lenses for any data type having a `JsonLike` typeclass instance. The get and
putback operations are implemented using zippers.

The partiality of the lenses is a result of the potential absence of expected elements in the JSON structure. `get`
and `set` operations return an `Option`, and `mod` operations which fail will return the original structure unmodified.

    import net.bmjames.json.Lenses._

    // A partial lens focusing on the string value of the 2nd element of field "soups"
    val firstSoup = field("soups") >=> elem(1) >=> strVal

    // The lens can be used simply to view the value at that location
    firstSoup.get(json)
    // Some(gumbo)

    // The lens can also be used to transform the value
    val updatedJson = firstSoup.mod("shellfish " + _, json)

    println(compact(render(updatedJson)))
    // {"soups":["goulash","shellfish gumbo","minestrone"]}

See [LensExamples](test/src/test/scala/com/gu/json/LensExamples.scala) for more examples.

[1]: http://en.wikipedia.org/wiki/Zipper_(data_structure)
[2]: http://json4s.org/
[3]: https://www.playframework.com/documentation/2.3.x/ScalaJson
