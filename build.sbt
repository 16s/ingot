import microsites.CdnDirectives

val catsVersion = "1.1.0"
val scalaTestVersion = "3.0.5"
val sharedDependencies = Seq()

val resultDependencies = sharedDependencies ++ Seq(
	"org.typelevel" %% "cats-core" % catsVersion,
	"org.typelevel" %% "cats-kernel" % catsVersion,
	"org.typelevel" %% "cats-macros" % catsVersion,
	"org.scalactic" %% "scalactic" % scalaTestVersion,
	"org.scalatest" %% "scalatest" % scalaTestVersion % "test",
	compilerPlugin("org.spire-math" %% "kind-projector" % "0.9.6"))

val resultStateDependencies = sharedDependencies ++ Seq(
	"com.chuusai" %% "shapeless" % "2.3.3",
	"org.scalactic" %% "scalactic" % scalaTestVersion,
	"org.scalatest" %% "scalatest" % scalaTestVersion % "test")

val sharedSettings = Seq(
  homepage := Some(url("https://16s.github.io/result")),
  description := "A simple library to handle effects, logs, errors and state",
  organization := "me.16s",
  organizationHomepage := Some(url("https://16s.github.io")),
  organizationName := "Tamas Neltz",
  scalaVersion := "2.12.6",
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-unchecked",
    "-explaintypes",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-Yno-adapted-args",                 // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
    "-Ypartial-unification",             // Enable partial unification in type constructor inference
    "-Ywarn-dead-code",                  // Warn when dead code is identified.
    "-Ywarn-inaccessible",               // Warn about inaccessible types in method signatures.
    "-Ywarn-infer-any",                  // Warn when a type argument is inferred to be `Any`.
    "-Ywarn-nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Ywarn-nullary-unit",               // Warn when nullary methods return Unit.
    "-Ywarn-numeric-widen",              // Warn when numerics are widened.
    "-Ywarn-value-discard"               // Warn when non-Unit expression results are unused.
  ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, major)) if major == 12 => Seq(
      "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
      "-Ywarn-unused:implicits",           // Warn if an implicit parameter is unused.
      "-Ywarn-unused:imports",             // Warn if an import selector is not referenced.
      "-Ywarn-unused:locals",              // Warn if a local definition is unused.
      "-Ywarn-unused:params",              // Warn if a value parameter is unused.
      "-Ywarn-unused:patvars",             // Warn if a variable bound in a pattern is unused.
      "-Ywarn-unused:privates",            // Warn if a private member is unused.
      "-Xlint:adapted-args",
      "-Xlint:by-name-right-associative",
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
      "-Xlint:type-parameter-shadow",      // A local type parameter shadows a type already in scope.
      "-Xlint:unsound-match"               // Pattern match may not be typesafe.
    )
    case _ => Seq()
  }),
  scalariformPreferences := scalariformPreferences.value,
  scalacOptions in (Compile,doc) ++= Seq("-groups", "-implicits"),
  crossScalaVersions := Seq("2.11.11", "2.12.6"),
  releaseCrossBuild := true,
  pomIncludeRepository := { _ => false },
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/16s/result"),
      "scm:git@github.com:16s/result.git"
    )
  ),
  developers := List(
    Developer(
      id    = "herczog",
      name  = "Tamas Neltz",
      email = "tamas@16s.me",
      url   = url("https://github.com/16s")
    )
  ),
  publishMavenStyle := true,
  licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  })

val codeSettings = sharedSettings ++ Seq(
  wartremoverErrors ++= Warts.unsafe
)

lazy val resultLib = (project in file("result")).settings(codeSettings, Seq(
	name := "result",
	libraryDependencies ++= resultDependencies,
  tutTargetDirectory := file("result"))).enablePlugins(TutPlugin)

lazy val resultState = (project in file("state")).dependsOn(resultLib).settings(codeSettings, Seq(
	name := "result-state",
	libraryDependencies ++= resultStateDependencies,
  tutTargetDirectory := file("state"))).enablePlugins(TutPlugin)

val micrositeSettings = Seq(
  micrositeName := "result",
  micrositeGithubOwner := "16s",
  micrositeGithubRepo := "result",
  micrositeGithubToken := sys.env.get("GITHUB_TOKEN"),
  micrositePushSiteWith := GitHub4s,
  micrositeHighlightTheme := "atelier-dune-light",
  micrositeGitterChannel := false,
  micrositeBaseUrl := "/result")

lazy val docs = (project in file("docs"))
  .settings(sharedSettings, micrositeSettings, Seq(publishArtifact := false)).dependsOn(resultLib, resultState)
  .enablePlugins(MicrositesPlugin)

lazy val readme = (project in file("readme"))
  .dependsOn(resultLib, resultState)
  .settings(sharedSettings, Seq(publishArtifact := false), tutTargetDirectory := file("."))
  .enablePlugins(TutPlugin)

lazy val result = (project in file("."))
  .settings(sharedSettings, Seq(publishArtifact := false), tutTargetDirectory := file("."))
  .aggregate(resultLib, resultState, docs, readme)



