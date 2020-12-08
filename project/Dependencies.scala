import sbt._
import Keys._

object Version {
  val jol = "0.5"
  val scriptedPlugin = "1.4.4"
}

object Dependencies {
  val jol            = "org.openjdk.jol" % "jol-core" % Version.jol
  val jolCli         = "org.openjdk.jol" % "jol-cli"  % Version.jol
  val scriptedPlugin = "org.scala-sbt" %% "scripted-plugin" % Version.scriptedPlugin
}

