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
    run in jol := runJolTask(fullClasspath in Compile).dependsOn(compile in Compile).evaluated,
    
    version in jol := "0.5",
    
    vmDetails in jol := runVmDetailsTask().evaluated,
    estimates in jol := runJolTask("estimates", fullClasspath in Compile).dependsOn(compile in Compile).evaluated,
    externals in jol := runJolTask("externals", fullClasspath in Compile).dependsOn(compile in Compile).evaluated,
    footprint in jol := runJolTask("footprint", fullClasspath in Compile).dependsOn(compile in Compile).evaluated,
    heapdump in jol  := runJolTask("heapdump",  fullClasspath in Compile).dependsOn(compile in Compile).evaluated,
    idealpack in jol := runJolTask("idealpack", fullClasspath in Compile).dependsOn(compile in Compile).evaluated,
    internals in jol := runJolTask("internals", fullClasspath in Compile).dependsOn(compile in Compile).evaluated,
    // TODO: stringCompress in jol <<= runJolTask("string-compress", fullClasspath in Compile).dependsOn(compile in Compile),

    discoveredClasses in jol := Seq.empty,
    // TODO tab auto-completion break if use `:=` and `.value`
    // https://github.com/sbt/sbt/issues/1444
    discoveredClasses in jol <<= (compile in Compile) map discoverAllClasses storeAs (discoveredClasses in jol) triggeredBy (compile in Compile)
  )  

  def runJolTask(classpath: Initialize[Task[Classpath]]): Initialize[InputTask[Unit]] = {
    val parser = loadForParser(discoveredClasses in jol)((s, names) => runJolModesParser(s, modes, names getOrElse Nil))
    Def.inputTask {
      val (mode, className, args) = parser.parsed
      runJol(streams.value.log, ivySbt.value, (version in jol).value, classpath.value, mode :: className :: args.toList)
    }
  }
  def runJolTask(mode: String, classpath: Initialize[Task[Classpath]]): Initialize[InputTask[Unit]] = {
    val parser = loadForParser(discoveredClasses in jol)((s, names) => runJolParser(s, names getOrElse Nil))
    Def.inputTask {
      val (className, args) = parser.parsed
      runJol(streams.value.log, ivySbt.value, (version in jol).value, classpath.value, mode :: className :: args.toList)
    }
  }

  def runJol(log: Logger, ivySbt: IvySbt, jolVersion: String, classpath: Classpath, args: Seq[String]): Unit = {
    val cpFiles = classpath.map(_.data)

    // TODO not needed, but at least confirms HERE we're able to see the class, sadly if we call JOL classes they won't...  
    //      val si = (scalaInstance in console).value
    //      val loader = sbt.classpath.ClasspathUtilities.makeLoader(cpFiles, si)
    //      val clazz = loader.loadClass(className) // make sure we can load it
    //      Thread.currentThread().setContextClassLoader(loader)

    val jolCoreJar = getArtifact("org.openjdk.jol" % "jol-core" % jolVersion, ivySbt, log)
    val jolCliJar = getArtifact("org.openjdk.jol" % "jol-cli" % jolVersion, ivySbt, log)
    val joptJar = getArtifact("net.sf.jopt-simple" % "jopt-simple" % "4.6", ivySbt, log) // TODO could be more nicely exposed as options
    val jolDeps = jolCliJar :: jolCoreJar :: joptJar :: Nil
    
    val allArg = s"${args.mkString(" ")} ${cpOption(cpFiles.toList)}"
    log.debug(s"jol: $allArg")

    import scala.sys.process._
    val javaClasspath = jolDeps.mkString(":") + ":" + cpFiles.toList.mkString(":")
    val output = s"java -cp $javaClasspath org.openjdk.jol.Main $allArg".!!(new ProcessLogger {
      override def buffer[T](f: => T): T = f
      override def out(s: => String): Unit = log.info(s)  
      override def err(s: => String): Unit = log.error(s)  
    })
    log.info(output)
    // TODO if anyone can figure out how to make jol not fail with ClassNotFound here that'd be grand (its tricky as it really wants to use the system loader...)  
    //      org.openjdk.jol.Main.main("estimates", className, cpOption(cpFiles))
  }

  /** 
   * From typesafehub/migration-manager (apache v2 licensed).
   * Resolves an artifact representing the previous abstract binary interface for testing.
   */
  def getArtifact(m: ModuleID, ivy: IvySbt, log: Logger): File = {
    val moduleSettings = InlineConfiguration(
      "dummy" % "test" % "version",
      ModuleInfo("dummy-test-project-for-resolving"),
      dependencies = Seq(m))
    val module = new ivy.Module(moduleSettings)
    val report = IvyActions.update(
      module,
      new UpdateConfiguration(
        retrieve = None,
        missingOk = false,
        logging = UpdateLogging.DownloadOnly),
      log)
    val optFile = (for {
      config <- report.configurations
      module <- config.modules
      (artifact, file) <- module.artifacts
      // TODO - Hardcode this?
      if artifact.name == m.name
    } yield file).headOption
    optFile getOrElse sys.error("Could not resolve JOL artifact: " + m)
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
    final val jol = sbt.config("jol") extend sbt.Configurations.CompileInternal  
    
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
