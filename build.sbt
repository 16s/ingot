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
  publishTo := {
    val path = "/repo/"
    if (isSnapshot.value)
      Some(Resolver.file("file", new File(Path.userHome.absolutePath + path + "/snapshot")))
    else
      Some(Resolver.file("file", new File(Path.userHome.absolutePath + path + "/release")))
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
  .settings(sharedSettings, micrositeSettings).dependsOn(resultLib, resultState)
  .enablePlugins(MicrositesPlugin)

lazy val readme = (project in file("readme"))
  .dependsOn(resultLib, resultState)
  .settings(sharedSettings, Seq(publishArtifact := false), tutTargetDirectory := file("."))
  .enablePlugins(TutPlugin)

lazy val result = (project in file("."))
  .settings(sharedSettings, Seq(publishArtifact := false), tutTargetDirectory := file("."))
  .aggregate(resultLib, resultState, docs, readme)



