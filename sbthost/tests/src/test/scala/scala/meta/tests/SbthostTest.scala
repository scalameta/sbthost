package scala.meta
package tests

import scala.meta.internal.semantic.{vfs => v}
import scala.meta.internal.semantic.{schema => s}

class SbthostTest extends org.scalatest.FunSuite {
  v.Database
    .load(Classpath(BuildInfo.targetroot.getAbsolutePath))
    .entries
    .foreach { entry =>
      test(entry.name.toString) {
        val sattrs = s.Attributes.parseFrom(entry.bytes)
        org.scalameta.logger.elem(sattrs)
        assert(sattrs.names.nonEmpty)
        val mattrs =
          new s.Database(List(sattrs))
            .toMeta(Some(Sourcepath(AbsolutePath(BuildInfo.sourceroot))))
        assert(mattrs.names.nonEmpty)
//      assert(mattrs.denotations.nonEmpty)
      }
    }
}
