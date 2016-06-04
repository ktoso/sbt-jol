import bintray.Keys._

sbtPlugin := true

organization := "pl.project13.sbt"
name := "sbt-jol"

scalaVersion := "2.10.6"
scalacOptions ++= List(
  "-unchecked",
  "-deprecation",
  "-language:_",
  "-target:jvm-1.6",
  "-encoding", "UTF-8"
)

libraryDependencies += Dependencies.jol
libraryDependencies += Dependencies.jolCli

publishTo <<= isSnapshot { snapshot =>
  if (snapshot) Some(Classpaths.sbtPluginSnapshots) else Some(Classpaths.sbtPluginReleases)
}

// publishing settings

publishMavenStyle := false
licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))
bintrayPublishSettings
repository in bintray := "sbt-plugins"
bintrayOrganization in bintray := None

pomExtra := <scm>
  <url>git@github.com:ktoso/sbt-jol.git</url>
  <connection>scm:git:git@github.com:ktoso/sbt-jol.git</connection>
</scm>
  <developers>
    <developer>
      <id>ktoso</id>
      <name>Konrad 'ktoso' Malawski</name>
      <url>http://kto.so</url>
    </developer>
  </developers>

scriptedSettings
scriptedLaunchOpts <+= version(v => s"-Dproject.version=$v")
