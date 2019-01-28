name := "auction"

version := "0.1.0"

scalaVersion := "2.12.8"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  guice,
  jdbc,
  evolutions,
  "org.postgresql" % "postgresql" % "42.2.4",
  "org.playframework.anorm" %% "anorm" % "2.6.1",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
)
