package scala.meta
package tests

import scala.meta.internal.semantic.{schema => s}
import scala.meta.internal.semantic.{vfs => v}
import scala.meta.sbthost.Sbthost

abstract class SbthostTest(expectedEntryPath: RelativePath, expectedSyntax: String)
    extends org.scalatest.FunSuite {

  test(expectedEntryPath.toString) {
    val entry = TestDatabase.entries.getOrElse(
      expectedEntryPath,
      sys.error(s"Missing entry for $expectedEntryPath")
    )
    val sattrs = s.Attributes.parseFrom(entry.bytes)
    assert(sattrs.names.nonEmpty)
    val mattrs = new s.Database(List(sattrs))
      .toMeta(Some(Sourcepath(AbsolutePath(BuildInfo.sourceroot))))
    val mirror = Sbthost.patchMirror(mattrs)
    assert(mirror.names.nonEmpty)
    val noDenotations = mirror.database.copy(entries = mirror.entries.map {
      case (input, attrs) =>
        input -> attrs.copy(denotations = Nil)
    })
    val syntax = noDenotations.syntax
    assert(syntax.trim == expectedSyntax.trim)

    mirror.sources.foreach { source =>
      source.collect {
        case name: Term.Name => mirror.names(name.pos) // error if not found
      }
    }
  }
}

class SbtScalaSourceTest
    extends SbthostTest(
      RelativePath(
        "META-INF/semanticdb/sbthost/input/src/main/scala/sbthost/CompiledWithSbthost.scala.semanticdb"),
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
         |""".stripMargin
    )

class SbtFileTest
    extends SbthostTest(
      RelativePath("META-INF/semanticdb/build.sbt.semanticdb"),
      s"""build.sbt
         |---------
         |Dialect:
         |Sbt0137
         |
         |Names:
         |[0..12): organization => _root_._empty_.
         |[13..15): := => _root_.sbt.internals.DslEntry.fromSettingsDef(Lsbt/Init/SettingsDefinition;)Lsbt/internals/DslEntry;.
         |""".stripMargin
    )

object TestDatabase {
  // Use '\\' to avoid illegal group reference when replacing
  final val StableHashId = "a4080ef5f8bfa310a7ce"
  final val StableTmpDir = "/tmp/scripted/"
  val entries: Map[RelativePath, v.Entry] = {
    val target = Classpath(BuildInfo.targetroot.getAbsolutePath)
    v.Database.load(target).entries.map(entry => (entry.name, entry)).toMap
  }
}
