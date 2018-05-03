
val catsVersion = "1.1.0"
val scalaTestVersion = "3.0.5"

val resultDependencies = Seq("org.typelevel" %% "cats-core" % catsVersion,
			"org.typelevel" %% "cats-kernel" % catsVersion,
			"org.typelevel" %% "cats-macros" % catsVersion,
			"org.scalactic" %% "scalactic" % scalaTestVersion,
			"org.scalatest" %% "scalatest" % scalaTestVersion % "test",
	compilerPlugin("org.spire-math" %% "kind-projector" % "0.9.6"))

val resultStateDependencies = Seq(
	"com.chuusai" %% "shapeless" % "2.3.3",
	"org.scalactic" %% "scalactic" % scalaTestVersion,
	"org.scalatest" %% "scalatest" % scalaTestVersion % "test")

val sharedSettings = Seq(
		organization := "net.16shells",
		scalaVersion := "2.12.5",
		version := "0.0.3",
		scalacOptions ++= Seq("-Ypartial-unification"), //, "-Xlog-implicits"
		wartremoverErrors ++= Warts.unsafe,
		scalariformPreferences := scalariformPreferences.value)

lazy val resultLib = (project in file("result")).settings(sharedSettings, Seq(
	name := "result",
	libraryDependencies ++= resultDependencies)).enablePlugins()

lazy val resultState = (project in file("state")).dependsOn(resultLib).settings(sharedSettings, Seq(
	name := "result-state",
	libraryDependencies ++= resultStateDependencies
))

lazy val results = (project in file(".")).aggregate(resultLib, resultState)


