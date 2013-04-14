package com.gu.json

import org.json4s.JsonAST._
import scalaz._


object Lenses {

  def field(name: String): JValue @?> JValue = mkPLens(_.field(name))

  def elem(index: Int): JValue @?> JValue = mkPLens(_.elem(index))

  def strVal: JValue @?> String = mkPLensP {
    case JString(s) => Store(JString.apply, s)
  }

  def intVal: JValue @?> BigInt = mkPLensP {
    case JInt(i) => Store(JInt.apply, i)
  }

  def doubleVal: JValue @?> Double = mkPLensP {
    case JDouble(d) => Store(JDouble.apply, d)
  }

  def decimalVal: JValue @?> BigDecimal = mkPLensP {
    case JDecimal(d) => Store(JDecimal.apply, d)
  }

  def boolVal: JValue @?> Boolean = mkPLensP {
    case JBool(b) => Store(JBool.apply, b)
  }

  def elems: JValue @?> List[JValue] = mkPLensP {
    case JArray(elems) => Store(JArray.apply, elems)
  }

  def fields: JValue @?> List[JField] = mkPLensP {
    case JObject(fields) => Store(JObject.apply, fields)
  }

  def mkPLensP[A](pfn: PartialFunction[JValue, Store[A, JValue]]): JValue @?> A =
    PLens(pfn.lift)

  def mkPLens(f: JCursor => Option[JCursor]): JValue @?> JValue =
    PLens { jValue =>
      for (c <- f(JCursor.jCursor(jValue))) yield Store(
        newFocus => c.replace(newFocus).toJValue,
        c.focus
      )
    }

  implicit class PStateOps[A, B](self: PState[A, B]) {
    /** OptionT monad transformer, specialized to partial lens state */
    def optionT = OptionT[({type λ[+α]=IndexedStateT[Id.Id, A, A, α]})#λ, B](self)
  }

}
