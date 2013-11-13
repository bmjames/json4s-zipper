package com.gu.json

import org.json4s.JsonAST._
import scalaz._, Scalaz._


object Lenses {

  def field[J : JsonLike](name: String): J @?> J = mkPLens(_.field(name))

  def elem[J : JsonLike](index: Int): J @?> J = mkPLens(_.elem(index))

  def strVal[J](implicit J: JsonLike[J]): J @?> String = PLens { j =>
    J.asString(j) map (s => Store(J.string, s))
  }

  def intVal[J](implicit J: JsonLike[J]): J @?> BigInt = PLens { j =>
    J.asInt(j) map (i => Store(J.int, i))
  }

  def doubleVal[J](implicit J: JsonLike[J]): J @?> Double = PLens { j =>
    J.asDouble(j) map (i => Store(J.double, i))
  }

//  def decimalVal: JValue @?> BigDecimal = mkPLensP {
//    case JDecimal(d) => Store(JDecimal.apply, d)
//  }

  def boolVal[J](implicit J: JsonLike[J]): J @?> Boolean = PLens { j =>
    J.asBool(j) map (b => Store(J.bool, b))
  }

  def elems[J](implicit J: JsonLike[J]): J @?> List[J] = PLens { j =>
    J.asArray(j) map (es => Store(J.array, es))
  }

  def fields[J](implicit J: JsonLike[J]): J @?> List[(String, J)] = PLens { j =>
    J.asObj(j) map (fs => Store(J.obj, fs))
  }

//  def mkPLensP[A](pfn: PartialFunction[JValue, Store[A, JValue]]): JValue @?> A =
//    PLens(pfn.lift)

  def mkPLens[J : JsonLike](f: Cursor[J] => Option[Cursor[J]]): J @?> J =
    PLens { jValue =>
      for (c <- f(Cursor.cursor(jValue))) yield Store(
        newFocus => c.replace(newFocus).toJson,
        c.focus
      )
    }

}

trait PStateSyntax {

  implicit class PStateOps[A, B](self: PState[A, B]) {
    
    /** OptionT monad transformer, specialized to partial lens state */
    def optionT = OptionT[({type λ[+α]=IndexedStateT[Id.Id, A, A, α]})#λ, B](self)

  }

}

object PStateSyntax extends PStateSyntax
