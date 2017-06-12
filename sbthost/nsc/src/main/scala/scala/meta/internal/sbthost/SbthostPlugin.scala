package scala.meta.internal.sbthost

import java.nio.file.Paths
import scala.meta.internal.sbthost
import scala.meta.internal.sbthost
import scala.meta.internal.semantic.{schema => s}
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent

// NOTE: This plugin is only intended to be used by sbt 0.13 for sbt 1.0 migration.
// Scalahost is a far superior plugin and should be used instead of sbthost.
class SbthostPlugin(val global: Global) extends Plugin with SbthostPipeline {
  val name = "sbthost"
  val description = "Compiler plugin for sbt v1.0 migration."
  val components = List[PluginComponent](SbthostComponent)

  // Hijacking reporter not work because:
  // [error] (sbthostInput/compile:compileIncremental) java.lang.ClassCastException:
  //   scala.meta.internal.sbthost.SbthostPlugin$$anon$2 cannot be cast to xsbt.DelegatingReporter
  //  hijackReporter()

  override def processOptions(options: List[String], error: (String) => Unit): Unit = {
    val SetSourceroot = "sourceroot:(.*)".r
    options.foreach {
      case SetSourceroot(sourceroot) =>
        config = config.copy(sourceroot = Paths.get(sourceroot))
      case els =>
        g.reporter.error(g.NoPosition, s"Ignoring unknown scalahost option $els")
    }
  }

}
