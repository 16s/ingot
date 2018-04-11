
val catsVersion = "1.0.1"
val scalaTestVersion = "3.0.5"

val dependencies = Seq("org.typelevel" %% "cats-core" % catsVersion,
			"org.typelevel" %% "cats-kernel" % catsVersion,
			"org.typelevel" %% "cats-macros" % catsVersion,
			"org.scalactic" %% "scalactic" % scalaTestVersion,
			"org.scalatest" %% "scalatest" % scalaTestVersion % "test")

val settings = Seq(
		name := "return",
		version := "net.16shells",
		scalaVersion := "2.12.5",
		version := "0.0.1-SNAPSHOT",
		scalacOptions += "-Ypartial-unification",
		libraryDependencies ++= dependencies,
		wartremoverErrors ++= Warts.unsafe,
		scalariformPreferences := scalariformPreferences.value)

lazy val result = (project in file(".")).settings(settings)
