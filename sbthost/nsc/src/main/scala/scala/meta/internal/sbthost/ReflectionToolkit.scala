package scala.meta.internal.sbthost

import scala.tools.nsc.Global

trait ReflectionToolkit {
  val global: Global
  lazy val g: global.type = global
}
