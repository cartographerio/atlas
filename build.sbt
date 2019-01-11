enablePlugins(spray.boilerplate.BoilerplatePlugin)

organization := "io.cartographer"
version      := "0.1.0"
scalaVersion := "2.12.4"

licenses += ("Apache-2.0", url("http://apache.org/licenses/LICENSE-2.0"))

lazy val commonCompilerSettings =
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4") ++
  Seq(scalacOptions ++= Seq(
    "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
    "-encoding", "utf-8",                // Specify character encoding used by source files.
    "-explaintypes",                     // Explain type errors in more detail.
    "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
    "-language:existentials",            // Existential types (besides wildcard types) can be written and inferred
    "-language:experimental.macros",     // Allow macro definition (besides implementation and application)
    "-language:higherKinds",             // Allow higher-kinded types
    "-language:implicitConversions",     // Allow definition of implicit functions called views
    "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
    "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
    "-Xfatal-warnings",                  // Fail the compilation if there are any warnings.
    "-Xfuture",                          // Turn on future language features.
    "-Xlint:adapted-args",               // Warn if an argument list is modified to match the receiver.
    "-Xlint:by-name-right-associative",  // By-name parameter of right associative operator.
    "-Xlint:constant",                   // Evaluation of a constant arithmetic expression results in an error.
    "-Xlint:delayedinit-select",         // Selecting member of DelayedInit.
    "-Xlint:doc-detached",               // A Scaladoc comment appears to be detached from its element.
    "-Xlint:inaccessible",               // Warn about inaccessible types in method signatures.
    "-Xlint:infer-any",                  // Warn when a type argument is inferred to be `Any`.
    "-Xlint:missing-interpolator",       // A string literal appears to be missing an interpolator id.
    "-Xlint:nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Xlint:nullary-unit",               // Warn when nullary methods return Unit.
    "-Xlint:option-implicit",            // Option.apply used implicit view.
    "-Xlint:package-object-classes",     // Class or object defined in package object.
    "-Xlint:poly-implicit-overload",     // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:private-shadow",             // A private field (or class parameter) shadows a superclass field.
    "-Xlint:stars-align",                // Pattern sequence wildcard must align with sequence component.
    "-Xlint:type-parameter-shadow",      // A local type parameter shadows a type already in env.
    "-Xlint:unsound-match",              // Pattern match may not be typesafe.
    "-Yno-adapted-args",                 // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
    "-Ypartial-unification",             // Enable partial unification in type constructor inference
    "-Ywarn-dead-code",                  // Warn when dead code is identified.
    "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
    "-Ywarn-inaccessible",               // Warn about inaccessible types in method signatures.
    "-Ywarn-infer-any",                  // Warn when a type argument is inferred to be `Any`.
    "-Ywarn-nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Ywarn-nullary-unit",               // Warn when nullary methods return Unit.
    "-Ywarn-numeric-widen",              // Warn when numerics are widened.
    // "-Ywarn-unused:implicits",           // Warn if an implicit parameter is unused.
    // "-Ywarn-unused:imports",             // Warn if an import selector is not referenced.
    // "-Ywarn-unused:locals",              // Warn if a local definition is unused.
    // "-Ywarn-unused:params",              // Warn if a value parameter is unused.
    // "-Ywarn-unused:patvars",             // Warn if a variable bound in a pattern is unused.
    // "-Ywarn-unused:privates",            // Warn if a private member is unused.
    "-Ywarn-value-discard"               // Warn when non-Unit expression results are unused.
  ))


lazy val publishSettings = Seq(
  publishTo := sonatypePublishTo.value,
  pomExtra in Global := {
    <url>https://github.com/cartographerio/atlas</url>
    <scm>
      <connection>scm:git:github.com/cartographerio/atlas</connection>
      <developerConnection>scm:git:git@github.com:cartographerio/atlas</developerConnection>
      <url>github.com/cartographerio/atlas</url>
    </scm>
    <developers>
      <developer>
        <id>davegurnell</id>
        <name>Dave Gurnell</name>
        <url>http://davegurnell.com</url>
        <organization>Cartographer</organization>
        <organizationUrl>http://cartographer.io</organizationUrl>
      </developer>
    </developers>
  }
)

lazy val noPublishSettings =
  Seq(
    publish := {},
    publishLocal := {},
    publishArtifact := false
  )

lazy val core = project.in(file("core"))
  .enablePlugins(spray.boilerplate.BoilerplatePlugin)
  .settings(publishSettings)
  .settings(commonCompilerSettings)
  .settings(
    name := "atlas-core",
    libraryDependencies ++= Seq(
      "org.apache.commons"    % "commons-lang3" % "3.2.1",
      "com.chuusai"          %% "shapeless"     % "2.3.3",
      "com.davegurnell"      %% "unindent"      % "1.1.0",
      "com.lihaoyi"          %% "fastparse"     % "1.0.0",
      "org.typelevel"        %% "cats-core"     % "1.0.0",
      "io.monix"             %% "minitest"      % "2.1.1" % Test
    ),
    testFrameworks += new TestFramework("minitest.runner.Framework"),
  )

lazy val benchmark = project.in(file("benchmark"))
  .dependsOn(core)
  .enablePlugins(JmhPlugin)
  .settings(noPublishSettings)
  .settings(commonCompilerSettings)
  .settings(
    name := "atlas-benchmark",
    libraryDependencies ++= Seq(
      "com.davegurnell" %% "unindent"  % "1.1.0",
      "org.python"       % "jython"    % "2.7.0"
    )
  )

lazy val root = project.in(file("."))
  .aggregate(core, benchmark)

addCommandAlias("bench", "benchmark/jmh:run -i 5 -wi 5 -f 1 -t 1")
