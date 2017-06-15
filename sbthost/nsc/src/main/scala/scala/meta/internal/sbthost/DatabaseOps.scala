package scala.meta.internal.sbthost

import scala.tools.nsc.Global

trait DatabaseOps
    extends SymbolOps
    with ConfigOps
    with InputOps
    with DenotationOps
    with HijackReporterOps
    with ReflectionToolkit {
  val global: Global
}
