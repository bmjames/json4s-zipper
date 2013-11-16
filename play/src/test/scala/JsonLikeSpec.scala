import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import play.api.libs.json.JsValue
import com.gu.json.scalacheck.ArbitraryInstances
import com.gu.json.JsonLikeLaws
import com.gu.json.play._

object JsonLikeSpec extends Properties("JsonLike[JsValue]") {

  val instances = ArbitraryInstances[JsValue]
  import instances._

  val laws = new JsonLikeLaws[JsValue]

  property("array") = forAll(laws.array _)

  property("obj") = forAll(laws.obj _)

  property("int") = forAll(laws.int _)

  property("bool") = forAll(laws.bool _)

  property("string") = forAll(laws.string _)

  property("double") = forAll(laws.double _)

}
