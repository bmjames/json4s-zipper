json4s-zipper
=============

This is an experimental [zipper][1] library for the [json4s][2] AST.

The goal of this library is to implement purely functional modifications to immutable JSON structures.

## Examples

This library depends on json4s-core, not on any of the json4s parsing libraries. To follow these examples, you'll need
a project that depends on `json4s-native` or `json4s-jackson`. (Or, you'll need to construct the JSON example by hand
using the AST.) If you have SBT installed, and the source for json4s-zipper, you can simply run `sbt test:console`.

To start with, here is some JSON, parsed into the `JValue` AST.

    import org.json4s.native.JsonMethods._
    
    val json = parse("""{"soups":["goulash","gumbo","minestrone"]}""")


### XPath-style syntax

This library has support for modifying elements of a JSON structure using an xpath-like syntax.

    import com.gu.json.syntax._
    import org.json4s.JsonAST._

    // Append the string " is tasty!" to each string in the array within the field "soups"
    val tastySoups = json.mod ("soups" \ *) { case JString(s) => JString(s + " is tasty!") }
    
    println(compact(render(tastySoups)))
    // {"soups":["goulash is tasty!","gumbo is tasty!","minestrone is tasty!"]}

### Basic Cursor API

It's a little verbose, but you can use the `JCursor` API directly. Most operations result in an `Option[JCursor]`, as
they may fail (e.g. if you use `field` when the focus is on a `JArray`).

    val cursor = json.cursor // Create a cursor focusing on the root of the `JValue`

    val updatedSoups = for {
      a <- cursor.field("soups")         // Go to field "soups"
      b <- a.prepend(JString("borscht")) // Prepend to the array
    } yield b

    val updatedJson = updatedSoups map (_.toJValue) // Get the resulting JSON

    for (j <- updatedJson) println(compact(render(j)))
    // {"soups":["borscht","goulash","gumbo","minestrone"]}

### Lenses

Lenses enable bidirectional transformations on data structures; i.e. the ability to query and update a *view* of the
structure, with modifications propagating back as changes to the original structure.

This library implements Scalaz partial lenses for `JValue` structures. The get and putback operations are implemented
using the cursor API, but lenses provide a more composable API, with a whole host of lens-related operations for free.

The partiality of the lenses is a result of the potential absence of expected elements in the `JValue` structure. Get
and set operations return an `Option`, and modify operations which fail will return the original structure unmodified.

    import com.gu.json.Lenses._

    // A partial lens focusing on the string value of the 2nd element of field "soups"
    val pLens = field("soups") >=> elem(1) >=> strVal

    // The lens can be used simply to view the value at that location
    pLens get json
    // Some(gumbo)

    // The lens can also be used to transform the value
    val updatedJson = pLens mod ("shellfish " + _, json)

    println(compact(render(updatedJson)))
    // {"soups":["goulash","shellfish gumbo","minestrone"]}

See [LensExamples][3] for more examples.

[1]: http://en.wikipedia.org/wiki/Zipper_(data_structure)
[2]: http://json4s.org/
[3]: https://github.com/bmjames/json4s-zipper/blob/master/src/test/scala/com/gu/json/LensExamples.scala
