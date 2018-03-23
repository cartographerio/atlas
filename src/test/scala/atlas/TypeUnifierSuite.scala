package atlas

import atlas.syntax._
import cats.implicits._
import minitest._
import unindent._

object TypeUnifierSuite extends SimpleTestSuite {
  test("compose") {
    val subst1 = Set(v(0) --> v(1))
    val subst2 = Set(v(1) --> v(2))

    val actual   = TypeUnifier.compose(subst1, subst2).toList.sorted
    val expected = List(v(0) --> v(2), v(1) --> v(2))

    assertEquals(actual, expected)
  }

  test("constant") {
    assertSuccess(
      expr"true",
      List(
        v(0) --> BoolType
      ))

    assertSuccess(
      expr"1",
      List(
        v(0) --> IntType
      ))
  }

  test("cond") {
    assertSuccess(
      expr"if true then 123 else 456",
      List(
        v(0) --> IntType,
        v(1) --> BoolType,
        v(2) --> IntType,
        v(3) --> IntType
      ))
  }

  test("infix") {
    assertSuccess(
      expr"123 + 456",
      List(
        v(0) --> IntType,
        v(1) --> IntType,
        v(2) --> IntType
      ))
  }

  test("prefix") {
    assertSuccess(
      expr"!!true",
      List(
        v(0) --> BoolType,
        v(1) --> BoolType,
        v(2) --> BoolType
      ))
  }

  test("cast") {
    assertSuccess(
      expr"1 : Int : Int",
      List(
        v(0) --> IntType,
        v(1) --> IntType,
        v(2) --> IntType))

    assertFailure(
      expr"1 : Int : Real",
      TypeMismatch(DblType, IntType))
  }

  test("block / let / ref") {
    assertSuccess(
      expr"""
      do
        1
        true
      end
      """,
      List(
        v(0) --> BoolType,
        v(1) --> IntType,
        v(2) --> BoolType))

    assertSuccess(
      expr"""
      do
        let a = 1
        let b = 2
        a > b
      end
      """,
      List(
        v(0) --> BoolType,
        v(1) --> IntType,
        v(2) --> IntType,
        v(3) --> IntType,
        v(4) --> IntType,
        v(5) --> BoolType))
  }

  test("func / apply") {
    assertSuccess(
      expr"n -> n > 0",
      List(
        v(0) --> FuncType(List(IntType), BoolType),
        v(1) --> IntType,
        v(2) --> BoolType,
        v(3) --> IntType))

    assertSuccess(
      expr"(a, b) -> a > b",
      List(
        v(0) --> FuncType(List(IntType, IntType), BoolType),
        v(1) --> IntType,
        v(2) --> IntType,
        v(3) --> BoolType))

    assertSuccess(
      expr"(a: Int -> String, b: Int) -> a(b)",
      List(
        v(0) --> FuncType(List(FuncType(List(IntType), StrType), IntType), StrType),
        v(1) --> FuncType(List(IntType), StrType),
        v(2) --> IntType,
        v(3) --> StrType))

    assertSuccess(
      prog"""
      let a = n -> n > 0
      a(10)
      """,
      List(
        v(0) --> BoolType,
        v(1) --> FuncType(List(IntType), BoolType),
        v(2) --> FuncType(List(IntType), BoolType),
        v(3) --> IntType,
        v(4) --> BoolType,
        v(5) --> IntType,
        v(6) --> BoolType,
        v(7) --> IntType))

    assertSuccess(
      expr"""
      do
        let a = n -> n + 1
        let b = n -> n > 0
        b(a(123))
      end
      """,
      List(
        v(0) --> BoolType,
        v(1) --> FuncType(List(IntType), IntType),
        v(2) --> FuncType(List(IntType), BoolType),
        v(3) --> FuncType(List(IntType), IntType),
        v(4) --> IntType,
        v(5) --> IntType,
        v(6) --> IntType,
        v(7) --> FuncType(List(IntType), BoolType),
        v(8) --> IntType,
        v(9) --> BoolType,
        v(10) --> IntType,
        v(11) --> BoolType,
        v(12) --> IntType,
        v(13) --> IntType))
  }

  test("block scope") {
    assertSuccess(
      expr"""
      do
        let a = 1
        do
          let a = 'hi'
          a
        end
        a
      end
      """,
      List(
        v(0) --> IntType,
        v(1) --> IntType,
        v(2) --> IntType,
        v(3) --> StrType,
        v(4) --> StrType,
        v(5) --> StrType))
  }

  test("func scope") {
    assertSuccess(
      expr"""
      do
        let add1 = n -> n + 1
        let apply = (a, b) -> a(b)
        apply(add1, 2)
      end
      """,
      List(
        v(0) --> IntType,
        v(1) --> FuncType(List(IntType), IntType),
        v(2) --> FuncType(List(FuncType(List(IntType), IntType), IntType), IntType),
        v(3) --> FuncType(List(IntType), IntType),
        v(4) --> IntType,
        v(5) --> IntType,
        v(6) --> IntType,
        v(7) --> FuncType(List(FuncType(List(IntType), IntType), IntType), IntType),
        v(8) --> FuncType(List(IntType), IntType),
        v(9) --> IntType,
        v(10) --> IntType,
        v(11) --> IntType,
        v(12) --> IntType))
  }

  test("mutual recursion") {
    assertSuccess(
      expr"""
      do
        let even = n -> if n == 0 then true  else odd(n - 1)
        let odd  = n -> if n == 0 then false else even(n - 1)
        even(10)
      end
      """,
      List(
        v(0) --> BoolType,
        v(1) --> FuncType(List(IntType), BoolType),
        v(2) --> FuncType(List(IntType), BoolType),
        v(3) --> FuncType(List(IntType), BoolType),
        v(4) --> IntType,
        v(5) --> BoolType,
        v(6) --> BoolType,
        v(7) --> IntType,
        v(8) --> BoolType,
        v(9) --> BoolType,
        v(10) --> IntType,
        v(11) --> IntType,
        v(12) --> FuncType(List(IntType), BoolType),
        v(13) --> IntType,
        v(14) --> BoolType,
        v(15) --> BoolType,
        v(16) --> IntType,
        v(17) --> BoolType,
        v(18) --> BoolType,
        v(19) --> IntType,
        v(20) --> IntType,
        v(21) --> BoolType,
        v(22) --> IntType))
  }

  def assertSuccess(expr: Expr, expected: List[Substitution], env: Env = Env.create): Unit = {
    val either = for {
      texpr       <- TypeAnnotator(expr)
      constraints <- TypeGenerator(texpr)
      actual      <- TypeUnifier(constraints)
    } yield (texpr, constraints, actual)

    either match {
      case Right((texpr, constraints, actual)) =>
        assert(
          actual == expected,
          i"""
          Incorrect results from type checking:
          expr = ${show"$expr"}
          texpr = ${show"$texpr"}
          constraints =
            ${constraints.mkString("\n  ")}
          actual =
            ${actual.mkString("\n  ")}
          expected =
            ${expected.mkString("\n  ")}
          """)

      case Left(error) =>
        fail(
          i"""
          Expected type checking to succeed, but it failed:
          $error
          """)

    }
  }

  def assertFailure(expr: Expr, expected: TypeError, env: Env = Env.create): Unit = {
    val either = for {
      texpr       <- TypeAnnotator(expr)
      constraints <- TypeGenerator(texpr)
      substs      <- TypeUnifier(constraints)
    } yield (texpr, constraints, substs)

    either match {
      case Right((texpr, constraints, substs)) =>
        fail(
          i"""
          Expected type checking to fail, but it succeeded:
          expr = ${show"$expr"}
          texpr = ${show"$texpr"}
          constraints =
            ${constraints.mkString("\n  ")}
          substs =
            ${substs.mkString("\n  ")}
          """)

      case Left(actual) =>
        assert(
          actual == expected,
          i"""
          Type checking failed with an unexpected error:
          expr = ${show"$expr"}
          actual = $actual
          expected = $expected
          """)

    }
  }

  def v(id: Int): TypeVar =
    TypeVar(id)
}
