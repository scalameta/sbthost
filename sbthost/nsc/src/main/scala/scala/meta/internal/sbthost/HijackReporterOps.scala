package scala.meta.internal.sbthost

import scala.reflect.internal.util.Position
import scala.tools.nsc.reporters.StoreReporter

trait HijackReporterOps { self: DatabaseOps =>
  def hijackReporter(): Unit = {
    g.reporter match {
      case s: StoreReporter =>
      case s =>
        val newReporter = new StoreReporter {
          override def info0(
              pos: Position,
              msg: String,
              severity: Severity,
              force: Boolean): Unit = {
            super.info0(pos, msg, severity, force)
            severity match {
              case INFO => s.info(pos, msg, force)
              case WARNING => s.warning(pos, msg)
              case ERROR => s.error(pos, msg)
              case _ =>
            }
          }
        }
        g.reporter = newReporter
    }
  }
}
