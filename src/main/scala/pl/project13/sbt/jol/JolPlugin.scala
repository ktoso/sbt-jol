package pl.project13.sbt.jol

import org.openjdk.jol.info.ClassLayout
import org.openjdk.jol.vm.VM
import sbinary.DefaultProtocol.StringFormat
import sbt.Attributed.data
import sbt.Cache.seqFormat
import sbt.Def.Initialize
import sbt.KeyRanks._
import sbt.complete.{DefaultParsers, Parser}
import sbt.Task
import sbt._ 
import sbt.Keys._
import xsbt.api.Discovery

object JolPlugin extends sbt.AutoPlugin {

  import autoImport._

  override def requires = sbt.plugins.JvmPlugin

  override def trigger = allRequirements

  override def projectSettings = Seq(
    vmDetails in jol <<= runVmDetailsTask(),

    run in jol <<= runJolTask(fullClasspath in Compile).dependsOn(compile in Compile),
    estimates in jol <<= runJolTask("estimates", fullClasspath in Compile).dependsOn(compile in Compile),
    externals in jol <<= runJolTask("externals", fullClasspath in Compile).dependsOn(compile in Compile),
    footprint in jol <<= runJolTask("footprint", fullClasspath in Compile).dependsOn(compile in Compile),
    heapdump in jol <<= runJolTask("heapdump", fullClasspath in Compile).dependsOn(compile in Compile),
    idealpack in jol <<= runJolTask("idealpack", fullClasspath in Compile).dependsOn(compile in Compile),
    internals in jol <<= runJolTask("internals", fullClasspath in Compile).dependsOn(compile in Compile),
    // TODO: stringCompress in jol <<= runJolTask("string-compress", fullClasspath in Compile).dependsOn(compile in Compile),

    discoveredClasses in jol := Seq.empty,
    discoveredClasses in jol <<= (compile in Compile) map discoverAllClasses storeAs (discoveredClasses in jol) triggeredBy (compile in Compile)
  )

  def runJolTask(classpath: Initialize[Task[Classpath]]): Initialize[InputTask[Unit]] = {
    val parser = loadForParser(discoveredClasses in jol)((s, names) => runJolModesParser(s, modes, names getOrElse Nil))
    Def.inputTask {
      val (mode, className, args) = parser.parsed
      runJol(streams.value.log, classpath.value, mode :: className :: args.toList)
    }
  }
  def runJolTask(mode: String, classpath: Initialize[Task[Classpath]]): Initialize[InputTask[Unit]] = {
    val parser = loadForParser(discoveredClasses in jol)((s, names) => runJolParser(s, names getOrElse Nil))
    Def.inputTask {
      val (className, args) = parser.parsed
      runJol(streams.value.log, classpath.value, mode :: className :: args.toList)
    }
  }

  def runJol(log: Logger, classpath: Classpath, args: Seq[String]): Unit = {
    val cpFiles = classpath.map(_.data)

    // TODO not needed, but at least confirms HERE we're able to see the class, sadly if we call JOL classes they won't...  
    //      val si = (scalaInstance in console).value
    //      val loader = sbt.classpath.ClasspathUtilities.makeLoader(cpFiles, si)
    //      val clazz = loader.loadClass(className) // make sure we can load it
    //      Thread.currentThread().setContextClassLoader(loader)

    val allArg = s"${args.mkString(" ")} ${cpOption(cpFiles)}"
    log.debug(s"jol: $allArg")

    import scala.sys.process._
    val output = s"java -jar /Users/ktoso/jol-cli-0.5-full.jar $allArg".!!
    log.info(output)
    // TODO if anyone can figure out how to make jol not fail with ClassNotFound here that'd be grand (its tricky as it really wants to use the system loader...)  
    //      org.openjdk.jol.Main.main("estimates", className, cpOption(cpFiles))
  }

  private def cpOption(cpFiles: Seq[File]): String = 
    "-cp " + cpFiles.mkString(":")  
  
  def runVmDetailsTask(): Initialize[InputTask[Unit]] = {
    Def.inputTask {
      streams.value.log.info(VM.current().details())
    }
  }

  private def discoverAllClasses(analysis: inc.Analysis): Seq[String] =
    Discovery.applications(Tests.allDefs(analysis)).collect({ case (definition, discovered) => definition.name })

  private def runJolParser: (State, Seq[String]) => Parser[(String, Seq[String])] = {
    import DefaultParsers._
    (state, mainClasses) => Space ~> token(NotSpace examples mainClasses.toSet) ~ spaceDelimited("<arg>")
  }
  private def runJolModesParser: (State, Seq[String], Seq[String]) => Parser[(String, String, Seq[String])] = {
    import DefaultParsers._
    (state, modes, mainClasses) =>
      val parser = Space ~> (token(NotSpace examples modes.toSet) ~ (Space ~> token(NotSpace examples mainClasses.toSet))) ~ spaceDelimited("<arg>")
      parser map { o => (o._1._1, o._1._2, o._2) }  
  }

  val modes = List(
    "estimates", 
    "externals", 
    "footprint", 
    "heapdump", 
    "idealpack", 
    "internals" // , 
    // TODO:   "stringCompress"
  )

  object autoImport {
    final val jol = sbt.config("jol") extend sbt.Compile
    
    lazy val vmDetails = inputKey[Unit]("Show vm details")

    lazy val estimates = inputKey[Unit]("Simulate the class layout in different VM modes.")
    lazy val externals = inputKey[Unit]("Show the object externals: the objects reachable from a given instance.")
    lazy val footprint = inputKey[Unit]("Estimate the footprint of all objects reachable from a given instance")
    lazy val heapdump = inputKey[Unit]("Consume the heap dump and estimate the savings in different layout strategies.")
    lazy val idealpack = inputKey[Unit]("Compute the object footprint under different field layout strategies.")
    lazy val internals = inputKey[Unit]("Show the object internals: field layout and default contents, object header")
    // TODO: lazy val stringCompress = inputKey[Unit]("Consume the heap dumps and figures out the savings attainable with compressed strings.")
    
    lazy val discoveredClasses = TaskKey[Seq[String]]("discovered-classes", "Auto-detects classes.")
  }

}
