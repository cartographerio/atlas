package atlas

import cats.implicits._

trait NativeImpl[F[_]] {
  self: Interpreter[F] =>

  import self.implicits._

  private val map: Value[F] =
    native.pure {
      (func: Value[F] => EvalStep[F, Value[F]], list: List[Value[F]]) =>
        list.traverse(func)
    }

  private val flatMap: Value[F] =
    native.pure {
      (func: Value[F] => EvalStep[F, List[Value[F]]], list: List[Value[F]]) =>
        list.flatTraverse(func)
    }

  private val filter: Value[F] =
    native.pure {
      (func: Value[F] => EvalStep[F, Boolean], list: List[Value[F]]) =>
        list
          .traverse(value => func(value).map(test => if(test) Some(value) else None))
          .map(_.flatten)
    }

  private val flatten: Value[F] =
    native {
      (list: List[List[Value[F]]]) =>
        list.flatten
    }

  def basicBindings: List[(String, Value[F])] =
    List(
      "map"     -> map,
      "flatMap" -> flatMap,
      "filter"  -> filter,
      "flatten" -> flatten,
    )
}
