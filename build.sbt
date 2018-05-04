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
  scalacOptions ++= Seq("-Ypartial-unification", "-feature", "-deprecation", "-unchecked"),
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



