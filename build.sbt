
val catsVersion = "1.1.0"
val scalaTestVersion = "3.0.5"

val dependencies = Seq("org.typelevel" %% "cats-core" % catsVersion,
			"org.typelevel" %% "cats-kernel" % catsVersion,
			"org.typelevel" %% "cats-macros" % catsVersion,
			"org.scalactic" %% "scalactic" % scalaTestVersion,
			"org.scalatest" %% "scalatest" % scalaTestVersion % "test")

val settings = Seq(
		name := "result",
		organization := "net.16shells",
		scalaVersion := "2.12.5",
		version := "0.0.2",
		scalacOptions ++= Seq("-Ypartial-unification"), //, "-Xlog-implicits"
		libraryDependencies ++= dependencies,
		wartremoverErrors ++= Warts.unsafe,
		scalariformPreferences := scalariformPreferences.value)

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.6")

lazy val result = (project in file(".")).settings(settings)
