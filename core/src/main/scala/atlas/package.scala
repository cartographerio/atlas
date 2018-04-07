import cats.data.StateT

package object atlas {
  type Env[F[_]] = ScopeChain[String, Value[F]]
  type EvalStep[F[_], A] = StateT[F, (Env[F], Limits), A]

  type TypeStep[F[_], A] = StateT[F, TypeEnv, A]
  type TypeEnv = ScopeChain[String, Type]
}
