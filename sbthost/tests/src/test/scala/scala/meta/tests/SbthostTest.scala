package scala.meta
package tests

import scala.meta.internal.semantic.{schema => s}
import scala.meta.internal.semantic.{vfs => v}
import scala.meta.sbthost.Sbthost

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
      val syntax = mirror.database.toString()
      assert(
        syntax.trim ==
          """|sbthost/input/src/main/scala/sbthost/CompiledWithSbthost.scala
             |--------------------------------------------------------------
             |Dialect:
             |Scala210
             |
             |Names:
             |[8..15): sbthost => _root_.sbthost.
             |[23..28): scala => _root_.scala.
             |[29..39): collection => _root_.scala.collection.
             |[40..49): immutable => _root_.scala.collection.immutable.
             |[62..81): CompiledWithSbthost => _root_.sbthost.CompiledWithSbthost.
             |[90..93): bar => _root_.sbthost.CompiledWithSbthost.bar(I)I.
             |[94..95): x => _root_.sbthost.CompiledWithSbthost.bar(I)I.(x)
             |[104..105): x => _root_.sbthost.CompiledWithSbthost.bar(I)I.(x)
             |[112..115): bar => _root_.sbthost.CompiledWithSbthost.bar(Ljava/lang/String;)Ljava/lang/String;.
             |[116..117): x => _root_.sbthost.CompiledWithSbthost.bar(Ljava/lang/String;)Ljava/lang/String;.(x)
             |[129..130): x => _root_.sbthost.CompiledWithSbthost.bar(Ljava/lang/String;)Ljava/lang/String;.(x)
             |[133..136): bar => _root_.sbthost.CompiledWithSbthost.bar(I)I.
             |[142..145): bar => _root_.sbthost.CompiledWithSbthost.bar(Ljava/lang/String;)Ljava/lang/String;.
             |""".stripMargin.trim)
      mirror.sources.foreach { source =>
        source.collect {
          case name: Term.Name => mirror.names(name.pos) // error if not found
        }
      }
    }
  }
}
