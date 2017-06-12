package scala.meta
package tests

import scala.meta.internal.semantic.{vfs => v}
import scala.meta.internal.semantic.{schema => s}
import scala.meta.sbthost.Sbthost
import org.scalameta.logger

class SbthostTest extends org.scalatest.FunSuite {
  val entries = v.Database
    .load(Classpath(BuildInfo.targetroot.getAbsolutePath))
    .entries
  entries.foreach { entry =>
    test(entry.name.toString) {
      val sattrs = s.Attributes.parseFrom(entry.bytes)
      assert(sattrs.names.nonEmpty)
      val mattrs =
        new s.Database(List(sattrs))
          .toMeta(Some(Sourcepath(AbsolutePath(BuildInfo.sourceroot))))
      val mirror = Sbthost.patchMirror(mattrs)
      org.scalameta.logger.elem(mirror)
      assert(mirror.names.nonEmpty)
      mirror.sources.foreach { source =>
        source.collect {
          case name: Term.Name => mirror.names(name.pos) // error if not found
        }
      }
    }
  }
}
