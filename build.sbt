
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
	organization := "me.16s",
	scalaVersion := "2.12.6",
	scalacOptions ++= Seq("-Ypartial-unification", "-feature", "-deprecation", "-unchecked"),
	wartremoverErrors ++= Warts.unsafe,
	scalariformPreferences := scalariformPreferences.value,
	scalacOptions in (Compile,doc) ++= Seq("-groups", "-implicits"),
	crossScalaVersions := Seq("2.11.11", "2.12.6"),
  publishTo := {
    val path = "/repo/"
    if (isSnapshot.value)
      Some(Resolver.file("file", new File(Path.userHome.absolutePath + path + "/snapshot")))
    else
      Some(Resolver.file("file", new File(Path.userHome.absolutePath + path + "/release")))
  })

lazy val resultLib = (project in file("result")).settings(sharedSettings, Seq(
	name := "result",
	libraryDependencies ++= resultDependencies)).enablePlugins()

lazy val resultState = (project in file("state")).dependsOn(resultLib).settings(sharedSettings, Seq(
	name := "result-state",
	libraryDependencies ++= resultStateDependencies
))

lazy val results = (project in file(".")).settings(sharedSettings, Seq(publishArtifact := false)).aggregate(resultLib, resultState)


