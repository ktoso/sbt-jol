import sbt.Keys.{libraryDependencies, licenses, name}

val myScriptedSettings = Seq(
  scriptedLaunchOpts += s"-Dproject.version=${version.value}"
  )

val defaultSettings = Seq(
  organization := "pl.project13.sbt",
  scalacOptions ++= List(
    "-unchecked",
    "-deprecation",
    "-language:_",
    "-encoding", "UTF-8"
    ),

  publishConfiguration := {
    val javaVersion = System.getProperty("java.specification.version")
    if (javaVersion != "1.8")
      throw new RuntimeException("Cancelling publish, please use JDK 1.8")
    publishConfiguration.value
  },

  libraryDependencies += Dependencies.jol,
  libraryDependencies += Dependencies.jolCli,
  libraryDependencies += Dependencies.scriptedPlugin
  )

lazy val root = (project in file("."))
  .settings(defaultSettings: _*)
  .enablePlugins(SbtPlugin)
  .settings(myScriptedSettings: _*)
  .settings(
    name := "sbt-jol",
    sbtPlugin := true,
    scalaVersion := "2.12.10",
    scalacOptions ++= List(
      "-unchecked",
      "-deprecation",
      "-language:_",
      "-target:jvm-1.8",
      "-encoding", "UTF-8"
      ),
    publishMavenStyle := false,
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html")),
    bintrayRepository := "sbt-plugins",
    bintrayOrganization in bintray := None,
    bintrayPackageLabels := Seq("sbt-multi-release-jar")
   )
