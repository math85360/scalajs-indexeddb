name := """scalajs-indexeddb"""

version := "0.0.1-SNAPSHOT"

organization := "com.iz2use"

scalaVersion := "2.11.8"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-language:implicitConversions")

lazy val root = project.in(file(".")).settings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.1"
    )
  )
  .enablePlugins(ScalaJSPlugin)

resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    sbt.Resolver.bintrayRepo("denigma", "denigma-releases"),
    "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
    "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    "Sonatype OSS Release Staging" at "https://oss.sonatype.org/content/groups/staging",
    Resolver.jcenterRepo
    )
