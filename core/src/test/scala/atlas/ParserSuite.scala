package atlas

import fastparse.all._
import minitest._
import unindent._

object UnitParserSuite extends SimpleTestSuite with AllParsers with ParserSuiteHelpers {
  test("digit") {
    object assert extends Assertions(digit)

    assert.complete("1", ())
    assert.failure("A", 0)
  }

  test("hexDigit") {
    object assert extends Assertions(hexDigit)

    assert.complete("1", ())
    assert.complete("A", ())
    assert.complete("f", ())
    assert.failure("G", 0)
  }

  test("newline") {
    object assert extends Assertions(newline)

    assert.complete("\r", ())
    assert.complete("\n", ())
    assert.complete("\r\n", ())
    assert.complete("\f", ())
    assert.partial("\n\r", (), 1)
  }

  test("whitespace") {
    object assert extends Assertions(whitespace)

    assert.complete(" ", ())
    assert.complete("\t", ())
    assert.failure("\r\n", 0)
    assert.failure("\n\r", 0)
  }

  test("ws") {
    object assert extends Assertions(ws)

    assert.complete(" \t \t ", ())
    assert.complete(" \n \n ", ())
    assert.complete(" \n \n \n ", ())
  }

  test("comment") {
    object assert extends Assertions(comment)

    assert.complete("# This is a comment", ())
    assert.partial("# This is a comment\n# So is this", (), 19)
  }

  test("escape") {
    object assert extends Assertions(escape)

    assert.complete("\\n", ())
    assert.complete("\\\\", ())
    assert.complete("\\\"", ())
    assert.failure("\\\n", 1)
  }
}

object TokenParserSuite extends SimpleTestSuite with AllParsers with ParserSuiteHelpers {
  test("boolean") {
    object assert extends Assertions(booleanToken)

    assert.complete("true", "true")
    assert.complete("false", "false")
    assert.partial("true false", "true", 4)
    assert.failure("truefalse", 0)
    assert.failure("maybe", 0)
  }

   test("intNumber") {
     object assert extends Assertions(intToken)

     assert.complete("123", "123")
     assert.partial("123.", "123", 3)
     assert.failure(".123", 0)
     assert.partial("123.456", "123", 3)
     assert.complete("+123", "+123")
     assert.failure("-.123", 1)
     assert.partial("123e456", "123", 3)
     assert.failure("-.123E-456", 1)
     assert.failure("letters", 0)
   }

  test("realNumber") {
    object assert extends Assertions(doubleToken)

    assert.failure("123", 0)
    assert.complete("123.", "123.")
    assert.complete(".123", ".123")
    assert.complete("123.456", "123.456")
    assert.failure("+123", 1)
    assert.complete("-.123", "-.123")
    assert.complete("123e456", "123e456")
    assert.complete("-.123E-456", "-.123E-456")
    assert.failure("letters", 0)
  }

  test("string") {
    object assert extends Assertions(stringToken)

    assert.complete("""'dave'""", "dave")
    assert.complete(""""has"""", "has")
    assert.complete(""""'escaped'"""", """'escaped'""")
    assert.complete("""'"escaped"'""", """"escaped"""")
    assert.partial("""'"abc'"def"""", """"abc""", 6)
  }

  test("ident") {
    object assert extends Assertions(ident)

    assert.complete("dave", "dave")
    assert.failure("if", 2)
    assert.complete("ifdave", "ifdave")
    assert.partial("dave was here", "dave", 4)
  }
}

object TypeParserSuite extends SimpleTestSuite with AllParsers with ParserSuiteHelpers {
  import PrefixOp._
  import InfixOp._

  object assert extends Assertions(tpe)

  test("literal") {
    assert.complete("Int", IntType)
    assert.complete("Real", DblType)
    assert.complete("String", StrType)
    assert.complete("Boolean", BoolType)
    assert.complete("Null", NullType)
  }

  test("ref") {
    assert.complete("A", TypeRef("A"))
  }

  test("func") {
    assert.complete(
      "Int -> String",
      FuncType(List(IntType), StrType))

    assert.complete(
      "(Int, String) -> Boolean",
      FuncType(List(IntType, StrType), BoolType))

    assert.complete(
      "Int -> String -> Boolean",
      FuncType(
        List(IntType),
        FuncType(
          List(StrType),
          BoolType)))

    assert.complete(
      "(Int, Real -> Int) -> (String, Real -> String) -> Boolean",
      FuncType(
        List(IntType, FuncType(List(DblType), IntType)),
        FuncType(
          List(StrType, FuncType(List(DblType), StrType)),
          BoolType)))
  }

  test("union") {
    assert.complete(
      "Boolean | String",
      BoolType | StrType)

    // TODO: Should function types be higher or lower precedence than union types?
    assert.complete(
      "Int -> Boolean | String",
      FuncType(List(IntType), BoolType | StrType))

    // TODO: Should function types be higher or lower precedence than union types?
    assert.complete(
      "(Int -> Boolean) | String",
      FuncType(List(IntType), BoolType) | StrType)
  }

  test("nullable") {
    assert.complete(
      "(String?)",
      StrType.?)

    assert.complete(
      "(String -> Int?)",
      FuncType(List(StrType), IntType.?))

    assert.complete(
      "(A?, B?) -> C?",
      FuncType(List(TypeRef("A").?, TypeRef("B").?), TypeRef("C").?))

    assert.complete(
      "(A? | B?)",
      TypeRef("A").? | TypeRef("B").?)

    assert.complete(
      "(Foo | Bar)?",
      TypeRef("Foo") | TypeRef("Bar") | NullType)
  }
}

object ExprParserSuite extends SimpleTestSuite with AllParsers with ParserSuiteHelpers {
  import PrefixOp._
  import InfixOp._

  object assert extends Assertions(expr)

  test("null") {
    assert.complete("null", NullExpr)
  }

  test("boolean") {
    assert.complete("true", BoolExpr(true))
    assert.complete("false", BoolExpr(false))
  }

  test("number") {
    assert.complete("123", IntExpr(123))
    assert.complete("123.456", DblExpr(123.456))
    assert.partial("123 . 456", IntExpr(123), 3)
  }

  test("string") {
    assert.complete("'dave'", StrExpr("dave"))
    assert.complete("\"dave\"", StrExpr("dave"))
    assert.complete("'\"dave\"'", StrExpr("\"dave\""))
    assert.complete("\"'dave'\"", StrExpr("'dave'"))
  }

  test("array") {
    assert.complete(
      "[ 1, 2 + 3, 4 ]",
      ArrExpr(List(IntExpr(1), InfixExpr(Add, IntExpr(2), IntExpr(3)), IntExpr(4))))

    assert.complete(
      "[1,2+3,4]",
      ArrExpr(List(IntExpr(1), InfixExpr(Add, IntExpr(2), IntExpr(3)), IntExpr(4))))

    assert.complete(
      "[ null , [ true && false ] , false ]",
      ArrExpr(List(NullExpr, ArrExpr(List(InfixExpr(And, BoolExpr(true), BoolExpr(false)))), BoolExpr(false))))

    assert.complete(
      "[null,[true&&false],false]",
      ArrExpr(List(NullExpr, ArrExpr(List(InfixExpr(And, BoolExpr(true), BoolExpr(false)))), BoolExpr(false))))
  }

  test("object") {
    assert.complete(
      "{ foo : null , \"'bar'\" : 1 + 2 , baz : true && false}",
      ObjExpr(List(
        "foo" -> NullExpr,
        "'bar'" -> InfixExpr(Add, IntExpr(1), IntExpr(2)),
        "baz" -> InfixExpr(And, BoolExpr(true), BoolExpr(false)))))

    assert.complete(
      "{foo:null,\"'bar'\":1+2,baz:true&&false}",
      ObjExpr(List(
        "foo" -> NullExpr,
        "'bar'" -> InfixExpr(Add, IntExpr(1), IntExpr(2)),
        "baz" -> InfixExpr(And, BoolExpr(true), BoolExpr(false)))))
  }

  test("ref") {
    assert.complete("i", RefExpr("i"))
    assert.failure("if", 0)
    assert.complete("iff", RefExpr("iff"))
  }

  test("cond") {
    assert.complete(
      "if a then b else c",
      CondExpr(
        RefExpr("a"),
        RefExpr("b"),
        RefExpr("c")))

    assert.complete(
      "ifathenbelsec",
      RefExpr("ifathenbelsec"))

    assert.complete(
      "if(a)then(b)else(c)",
      CondExpr(
        RefExpr("a"),
        RefExpr("b"),
        RefExpr("c")))

    assert.complete(
      "if a > b then c + d else e + f",
      CondExpr(
        InfixExpr(Gt, RefExpr("a"), RefExpr("b")),
        InfixExpr(Add, RefExpr("c"), RefExpr("d")),
        InfixExpr(Add, RefExpr("e"), RefExpr("f"))))
  }

  test("cast") {
    assert.complete(
      "123 : Int",
      CastExpr(IntExpr(123), IntType))

    assert.complete(
      "123 + 234: Int",
      InfixExpr(
        InfixOp.Add,
        IntExpr(123),
        CastExpr(IntExpr(234), IntType)))

    assert.complete(
      "(123 + 234): Int",
      CastExpr(
        InfixExpr(
          InfixOp.Add,
          IntExpr(123),
          IntExpr(234)),
        IntType))

    assert.complete(
      "123 : Int | String",
      CastExpr(
        IntExpr(123),
        IntType | StrType))

    assert.complete(
      "123 : (Int | String)",
      CastExpr(
        IntExpr(123),
        IntType | StrType))
  }

  test("call") {
    assert.complete(
      "add ( a , b , c )",
      AppExpr(RefExpr("add"), List(RefExpr("a"), RefExpr("b"), RefExpr("c"))))

    assert.complete(
      "add(a,b,c)",
      AppExpr(RefExpr("add"), List(RefExpr("a"), RefExpr("b"), RefExpr("c"))))
  }

  test("paren") {
    assert.complete("( a )", RefExpr("a"))
    assert.complete("(a)", RefExpr("a"))
  }

  test("prefix") {
    assert.complete("- a", PrefixExpr(Neg, RefExpr("a")))
    assert.complete("+a", PrefixExpr(Pos, RefExpr("a")))
    assert.complete("!a", PrefixExpr(Not, RefExpr("a")))

    assert.complete(
      "+ a + + b",
      InfixExpr(
        Add,
        PrefixExpr(Pos, RefExpr("a")),
        PrefixExpr(Pos, RefExpr("b"))))
  }

  test("infix") {
    assert.complete("a || b", InfixExpr(Or, RefExpr("a"), RefExpr("b")))
    assert.complete("a && b", InfixExpr(And, RefExpr("a"), RefExpr("b")))
    assert.complete("a == b", InfixExpr(Eq, RefExpr("a"), RefExpr("b")))
    assert.complete("a != b", InfixExpr(Ne, RefExpr("a"), RefExpr("b")))
    assert.complete("a > b", InfixExpr(Gt, RefExpr("a"), RefExpr("b")))
    assert.complete("a < b", InfixExpr(Lt, RefExpr("a"), RefExpr("b")))
    assert.complete("a >= b", InfixExpr(Gte, RefExpr("a"), RefExpr("b")))
    assert.complete("a <= b", InfixExpr(Lte, RefExpr("a"), RefExpr("b")))
    assert.complete("a + b", InfixExpr(Add, RefExpr("a"), RefExpr("b")))
    assert.complete("a - b", InfixExpr(Sub, RefExpr("a"), RefExpr("b")))
    assert.complete("a * b", InfixExpr(Mul, RefExpr("a"), RefExpr("b")))
    assert.complete("a / b", InfixExpr(Div, RefExpr("a"), RefExpr("b")))

    assert.complete("a||b", InfixExpr(Or, RefExpr("a"), RefExpr("b")))
    assert.complete("a&&b", InfixExpr(And, RefExpr("a"), RefExpr("b")))
    assert.complete("a==b", InfixExpr(Eq, RefExpr("a"), RefExpr("b")))
    assert.complete("a!=b", InfixExpr(Ne, RefExpr("a"), RefExpr("b")))
    assert.complete("a>b", InfixExpr(Gt, RefExpr("a"), RefExpr("b")))
    assert.complete("a<b", InfixExpr(Lt, RefExpr("a"), RefExpr("b")))
    assert.complete("a>=b", InfixExpr(Gte, RefExpr("a"), RefExpr("b")))
    assert.complete("a<=b", InfixExpr(Lte, RefExpr("a"), RefExpr("b")))
    assert.complete("a+b", InfixExpr(Add, RefExpr("a"), RefExpr("b")))
    assert.complete("a-b", InfixExpr(Sub, RefExpr("a"), RefExpr("b")))
    assert.complete("a*b", InfixExpr(Mul, RefExpr("a"), RefExpr("b")))
    assert.complete("a/b", InfixExpr(Div, RefExpr("a"), RefExpr("b")))

    assert.complete(
      "a + b + c",
      InfixExpr(
        Add,
        InfixExpr(
          Add,
          RefExpr("a"),
          RefExpr("b")),
        RefExpr("c")))

    assert.complete(
      "a * b + c",
      InfixExpr(
        Add,
        InfixExpr(
          Mul,
          RefExpr("a"),
          RefExpr("b")),
        RefExpr("c")))

    assert.complete(
      "a + b * c",
      InfixExpr(
        Add,
        RefExpr("a"),
        InfixExpr(
          Mul,
          RefExpr("b"),
          RefExpr("c"))))

    assert.complete(
      "( a + b ) * c",
      InfixExpr(
        Mul,
        InfixExpr(
          Add,
          RefExpr("a"),
          RefExpr("b")),
        RefExpr("c")))

    assert.complete(
      "a * (b + c)",
      InfixExpr(
        Mul,
        RefExpr("a"),
        InfixExpr(
          Add,
          RefExpr("b"),
          RefExpr("c"))))

    assert.complete(
      "a <= b && c >= d",
      InfixExpr(
        And,
        InfixExpr(
          Lte,
          RefExpr("a"),
          RefExpr("b")),
        InfixExpr(
          Gte,
          RefExpr("c"),
          RefExpr("d"))))

    assert.complete(
      "(a) + (b)",
      InfixExpr(
        Add,
        RefExpr("a"),
        RefExpr("b")))

    assert.complete(
      "+a + +b",
      InfixExpr(
        Add,
        PrefixExpr(Pos, RefExpr("a")),
        PrefixExpr(Pos, RefExpr("b"))))
  }

  test("select") {
    assert.complete(
      "a . b",
      SelectExpr(RefExpr("a"), "b"))

    assert.complete(
      "a.b.c",
      SelectExpr(SelectExpr(RefExpr("a"), "b"), "c"))

    assert.complete(
      "a.b+c.d",
      InfixExpr(
        Add,
        SelectExpr(RefExpr("a"), "b"),
        SelectExpr(RefExpr("c"), "d")))
  }

  test("block") {
    assert.complete(
      "do a end",
      BlockExpr(Nil, RefExpr("a")))

    assert.complete(
      "doaend",
      RefExpr("doaend"))

    assert.complete(
      i"""do a
      b end
      """,
      BlockExpr(
        List(ExprStmt(RefExpr("a"))),
        RefExpr("b")))

    assert.failure(
      "do let a = 1 end",
      16)

    assert.complete(
      i"""
      do
        a
        b
        c
      end
      """,
      BlockExpr(
        List(
          ExprStmt(RefExpr("a")),
          ExprStmt(RefExpr("b"))),
        RefExpr("c")))

    assert.complete(
      "do;a;b;c;end",
      BlockExpr(
        List(
          ExprStmt(RefExpr("a")),
          ExprStmt(RefExpr("b"))),
        RefExpr("c")))

    assert.failure("do a b c end", 0)
  }

  test("func") {
    assert.complete(
      "( a, b ) -> a + b",
      FuncExpr(
        List(FuncArg("a", None), FuncArg("b", None)),
        None,
        InfixExpr(Add, RefExpr("a"), RefExpr("b"))))
    assert.complete(
      "( a , b : String ) : Int -> a + b",
      FuncExpr(
        List(FuncArg("a", None), FuncArg("b", Some(StrType))),
        Some(IntType),
        InfixExpr(Add, RefExpr("a"), RefExpr("b"))))

    assert.complete(
      "(a:Int,b):Real->a+b",
      FuncExpr(
        List(FuncArg("a", Some(IntType)), FuncArg("b", None)),
        Some(DblType),
        InfixExpr(Add, RefExpr("a"), RefExpr("b"))))

    assert.complete(
      "a -> b -> a + b",
      FuncExpr(
        List(FuncArg("a", None)),
        None,
        FuncExpr(
          List(FuncArg("b", None)),
          None,
          InfixExpr(Add, RefExpr("a"), RefExpr("b")))))

    assert.complete(
      "a -> b -> a.c + b.d",
      FuncExpr(
        List(FuncArg("a", None)),
        None,
        FuncExpr(
          List(FuncArg("b", None)),
          None,
          InfixExpr(Add,
            SelectExpr(RefExpr("a"), "c"),
            SelectExpr(RefExpr("b"), "d")))))
  }
}

object StmtParserSuite extends SimpleTestSuite with AllParsers with ParserSuiteHelpers {
  object assert extends Assertions(stmt)

  test("let") {
    assert.complete("let a = b", LetStmt("a", None, RefExpr("b")))

    assert.complete("let a = b -> c", LetStmt(
      "a",
      None,
      FuncExpr(
        List(FuncArg("b", None)),
        None,
        RefExpr("c"))))

    assert.complete("let a: Int = b", LetStmt(
      "a",
      Some(IntType),
      RefExpr("b")))

    assert.complete(
      i"""
      let add = ( a, b ) -> a + b
      """,
      LetStmt(
        "add",
        None,
        FuncExpr(
          List(FuncArg("a", None), FuncArg("b", None)),
          None,
          InfixExpr(InfixOp.Add, RefExpr("a"), RefExpr("b")))))
  }

  test("expr") {
    assert.complete(
      "a + b",
      ExprStmt(InfixExpr(InfixOp.Add, RefExpr("a"), RefExpr("b"))))
  }
}

trait ParserSuiteHelpers {
  self: SimpleTestSuite =>

  class Assertions[A](parser: P[A]) {
    def complete(input: String, expected: A): Unit =
      partial(input, expected, input.length)

    def partial(input: String, expected: A, index: Int): Unit =
      parser.parse(input) match {
        case Parsed.Success(actual, n) =>
          assertEquals(actual, expected)
          assertEquals(n, index)
        case Parsed.Failure(_, _, _) =>
          fail(s"Could not parse input: [$input]")
      }

    def failure(input: String, index: Int): Unit =
      parser.parse(input) match {
        case Parsed.Success(value, _) =>
          fail(s"Expected parsing to fail: $input => $value")
        case Parsed.Failure(_, i, _) =>
          assertEquals(i, index)
      }
  }
}