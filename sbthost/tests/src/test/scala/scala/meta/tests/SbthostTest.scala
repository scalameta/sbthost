package scala.meta
package tests

import scala.meta.internal.semantic.{schema => s}
import scala.meta.internal.semantic.{vfs => v}
import scala.meta.sbthost.Sbthost
import TestDatabase.{StableHashId, StableTmpDir}

abstract class SbthostTest(expectedEntryPath: RelativePath, expectedSyntax: String)
    extends org.scalatest.FunSuite {
  def preProcessSyntax(syntax: String): String = syntax
  def optionalDialect: Option[Dialect] = None

  // Reimplement mirror.sources to inject our own dialect if defined
  def sources(mirror: Mirror): Seq[Source] = {
    mirror.database.entries.map {
      case (input, attrs) =>
        val preferredDialect = optionalDialect.getOrElse(attrs.dialect)
        preferredDialect(input).parse[Source].get
    }
  }

  test(expectedEntryPath.toString) {
    val entry = TestDatabase.entries
      .getOrElse(expectedEntryPath, sys.error(s"Missing entry for $expectedEntryPath"))
    val sattrs = s.Attributes.parseFrom(entry.bytes)
    assert(sattrs.names.nonEmpty)
    val mattrs = new s.Database(List(sattrs))
      .toMeta(Some(Sourcepath(AbsolutePath(BuildInfo.sourceroot))))
    val mirror = Sbthost.patchMirror(mattrs)
    org.scalameta.logger.elem(mirror)
    assert(mirror.names.nonEmpty)
    val syntax = mirror.database.toString()
    assert(preProcessSyntax(syntax.trim) == expectedSyntax.trim)

    sources(mirror).foreach { source =>
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
         |
         |Denotations:
         |_root_.java.lang.Object#`<init>`()V. => primaryctor <init>: ()Object
         |_root_.sbthost. => package sbthost
         |_root_.sbthost.CompiledWithSbthost. => final object CompiledWithSbthost
         |_root_.sbthost.CompiledWithSbthost.`<init>`()V. => primaryctor <init>: ()sbthost.CompiledWithSbthost.type
         |_root_.sbthost.CompiledWithSbthost.bar(I)I. => def bar: (x: Int)Int
         |_root_.sbthost.CompiledWithSbthost.bar(I)I.(x) => param x: Int
         |_root_.sbthost.CompiledWithSbthost.bar(Ljava/lang/String;)Ljava/lang/String;. => def bar: (x: String)String
         |_root_.sbthost.CompiledWithSbthost.bar(Ljava/lang/String;)Ljava/lang/String;.(x) => param x: String
         |_root_.scala. => package scala
         |_root_.scala.AnyRef# => val AnyRef: Specializable
         |_root_.scala.collection. => package collection
         |_root_.scala.collection.immutable. => package immutable
         |file://CompiledWithSbthost.scala@82..82 => val <local CompiledWithSbthost>: <notype>
         |""".stripMargin
    )

class SbtFileTest
    extends SbthostTest(
      RelativePath("META-INF/semanticdb/build.sbt.semanticdb"),
      s"""build.sbt
         |---------
         |Dialect:
         |Scala210
         |
         |Names:
         |[0..12): organization => _root_._empty_.
         |[13..15): := => _root_.sbt.internals.DslEntry.fromSettingsDef(Lsbt/Init/SettingsDefinition;)Lsbt/internals/DslEntry;.
         |
         |Denotations:
         |_root_._empty_. => package <empty>
         |_root_._empty_.$StableHashId. => final object $StableHashId
         |_root_._empty_.$StableHashId. => final object $StableHashId
         |_root_._empty_.$StableHashId.$$sbtdef()Lsbt/internals/DslEntry;. => def $$sbtdef: sbt.internals.DslEntry
         |_root_._empty_.$StableHashId.`<init>`()V. => primaryctor <init>: ()$StableHashId.type
         |_root_.java.lang.Object#`<init>`()V. => primaryctor <init>: ()Object
         |_root_.sbt. => package sbt
         |_root_.sbt.Keys. => final object Keys
         |_root_.sbt.Keys.organization. => val organization: sbt.SettingKey[String]
         |_root_.sbt.LinePosition. => final object LinePosition
         |_root_.sbt.LinePosition.apply(Ljava/lang/String;I)Lsbt/LinePosition;. => case def apply: (path: String, startLine: Int)sbt.LinePosition
         |_root_.sbt.Scoped.DefinableSetting#set(Lsbt/Init/Initialize;Lsbt/SourcePosition;)Lsbt/Init/Setting;. => final def set: (app: sbt.Def.Initialize[S], source: sbt.SourcePosition)sbt.Def.Setting[S]
         |_root_.sbt.dsl. => package dsl
         |_root_.sbt.internals. => package internals
         |_root_.sbt.internals.DslEntry. => final object DslEntry
         |_root_.sbt.internals.DslEntry.fromSettingsDef(Lsbt/Init/SettingsDefinition;)Lsbt/internals/DslEntry;. => implicit def fromSettingsDef: (inc: sbt.Def.SettingsDefinition)sbt.internals.DslEntry
         |_root_.sbt.plugins. => package plugins
         |_root_.sbt.std.InitializeInstance. => final object InitializeInstance
         |_root_.sbt.std.InitializeInstance.pure(Lscala/Function0;)Lsbt/Init/Initialize;. => def pure: [T](t: () => T)sbt.Def.Initialize[T]
         |_root_.scala. => package scala
         |_root_.scala.AnyRef# => val AnyRef: Specializable
         |file://${StableTmpDir}basic/build.sbt@0..0 => val <local $StableHashId>: <notype>
         |file://${StableTmpDir}basic/build.sbt@16..16 => val $$anonfun: <notype>
  """.stripMargin
    ) {
  override final val optionalDialect = Some(dialects.Sbt0137)
  private final val hashRegex = "\\$[0-9a-fA-F]{20}".r
  private final val scriptedTmpDirRegex = "/tmp/sbt_[0-9a-f]{8}/".r
  override def preProcessSyntax(syntax: String): String = {
    val noUnstableHashes = hashRegex.replaceAllIn(syntax, StableHashId)
    scriptedTmpDirRegex.replaceAllIn(noUnstableHashes, StableTmpDir)
  }
}

object TestDatabase {
  // Use '\\' to avoid illegal group reference when replacing
  final val StableHashId = "a4080ef5f8bfa310a7ce"
  final val StableTmpDir = "/tmp/scripted/"
  val entries: Map[RelativePath, v.Entry] = {
    val target = Classpath(BuildInfo.targetroot.getAbsolutePath)
    v.Database.load(target).entries.map(entry => (entry.name, entry)).toMap
  }
}
