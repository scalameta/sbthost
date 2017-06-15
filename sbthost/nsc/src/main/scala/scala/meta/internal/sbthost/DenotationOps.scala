package scala.meta.internal.sbthost

import scala.reflect.internal.{Flags => gf}
import scala.meta.internal.semantic.{schema => s}

object mf {
  final val VAL: Long = 1 << 0
  final val VAR: Long = 1 << 1
  final val DEF: Long = 1 << 2
  final val PRIMARYCTOR: Long = 1 << 3
  final val SECONDARYCTOR: Long = 1 << 4
  final val MACRO: Long = 1 << 5
  final val TYPE: Long = 1 << 6
  final val PARAM: Long = 1 << 7
  final val TYPEPARAM: Long = 1 << 8
  final val OBJECT: Long = 1 << 9
  final val PACKAGE: Long = 1 << 10
  final val PACKAGEOBJECT: Long = 1 << 11
  final val CLASS: Long = 1 << 12
  final val TRAIT: Long = 1 << 13
  final val PRIVATE: Long = 1 << 14
  final val PROTECTED: Long = 1 << 15
  final val ABSTRACT: Long = 1 << 16
  final val FINAL: Long = 1 << 17
  final val SEALED: Long = 1 << 18
  final val IMPLICIT: Long = 1 << 19
  final val LAZY: Long = 1 << 20
  final val CASE: Long = 1 << 21
  final val COVARIANT: Long = 1 << 22
  final val CONTRAVARIANT: Long = 1 << 23
  final val INLINE: Long = 1 << 24
}

trait DenotationOps { self: DatabaseOps =>
  import g._

  implicit class XtensionGSymbolMDenotation(gsym0: g.Symbol) {
    private val gsym: g.Symbol = {
      if (gsym0.isModuleClass) gsym0.asClass.module
      else if (gsym0.isTypeSkolem) gsym0.deSkolemize
      else gsym0.setter(gsym0.owner).orElse(gsym0.getter(gsym0.owner).orElse(gsym0))
    }

    private val isObject = gsym.isModule && !gsym.hasFlag(gf.PACKAGE) && gsym.name != nme.PACKAGE

    private def definitionFlags: Long = {
      var flags = 0l
      def maybeValOrVar =
        (gsym.isTerm && flags == 0l) || (gsym.hasFlag(gf.PARAMACCESSOR) && flags == mf.PARAM)
      if (gsym.isMethod && !gsym.isConstructor && !gsym.hasFlag(gf.MACRO) && !gsym.hasFlag(
            gf.ACCESSOR) && !gsym.hasFlag(gf.PARAMACCESSOR)) flags |= mf.DEF
      if (gsym.isPrimaryConstructor) flags |= mf.PRIMARYCTOR
      if (gsym.isConstructor && !gsym.isPrimaryConstructor) flags |= mf.SECONDARYCTOR
      if (gsym.hasFlag(gf.MACRO)) flags |= mf.MACRO
      if (gsym.isType && !gsym.isClass && !gsym.hasFlag(gf.PARAM)) flags |= mf.TYPE
      if (gsym.isTerm && (gsym.hasFlag(gf.PARAM) || gsym.hasFlag(gf.PARAMACCESSOR)))
        flags |= mf.PARAM
      if (gsym.isType && gsym.hasFlag(gf.PARAM)) flags |= mf.TYPEPARAM
      if (isObject) flags |= mf.OBJECT
      if (gsym.hasFlag(gf.PACKAGE)) flags |= mf.PACKAGE
      if (gsym.isModule && gsym.name == nme.PACKAGE) flags |= mf.PACKAGEOBJECT
      if (gsym.isClass && !gsym.hasFlag(gf.TRAIT)) flags |= mf.CLASS
      if (gsym.isClass && gsym.hasFlag(gf.TRAIT)) flags |= mf.TRAIT
      if (maybeValOrVar && (gsym.hasFlag(gf.MUTABLE) || nme.isSetterName(gsym.name)))
        flags |= mf.VAR
      if (maybeValOrVar && !(gsym.hasFlag(gf.LOCAL) && gsym.hasFlag(gf.PARAMACCESSOR)))
        flags |= mf.VAL
      flags
    }

    private def accessQualifierFlags: Long = {
      var flags = 0l
      val gpriv = gsym.privateWithin.orElse(gsym.owner)
      if (gsym.hasFlag(gf.SYNTHETIC) && gsym.hasFlag(gf.ARTIFACT)) {
        // NOTE: some sick artifact vals produced by mkPatDef can be private to method (whatever that means)
        // so here we just ignore them
      } else {
        if (gsym.hasFlag(gf.PROTECTED)) flags |= mf.PROTECTED
        if (gsym.hasFlag(gf.PRIVATE) && !gsym.hasFlag(gf.PARAMACCESSOR)) flags |= mf.PRIVATE
        // TODO: `private[pkg] class C` doesn't have PRIVATE in its flags
        // so we need to account for that!
        if (gsym.hasAccessBoundary && gpriv != g.NoSymbol && !gsym.hasFlag(gf.PROTECTED))
          flags |= mf.PRIVATE
      }
      flags
    }

    private def otherFlags: Long = {
      var flags = 0l
      val isDeclaredDeferred = gsym.hasFlag(gf.DEFERRED) && !gsym.hasFlag(gf.PARAM)
      val isDeclaredAbstract = (gsym.hasFlag(gf.ABSTRACT) && !gsym.hasFlag(gf.TRAIT)) || gsym
        .hasFlag(gf.ABSOVERRIDE)
      if (isDeclaredDeferred || isDeclaredAbstract) flags |= mf.ABSTRACT
      if ((gsym.hasFlag(gf.FINAL) && !gsym.hasFlag(gf.PACKAGE)) || isObject) flags |= mf.FINAL
      if (gsym.hasFlag(gf.SEALED)) flags |= mf.SEALED
      if (gsym.hasFlag(gf.IMPLICIT)) flags |= mf.IMPLICIT
      if (gsym.hasFlag(gf.LAZY)) flags |= mf.LAZY
      if (gsym.hasFlag(gf.CASE)) flags |= mf.CASE
      if (gsym.isType && gsym.hasFlag(gf.CONTRAVARIANT)) flags |= mf.CONTRAVARIANT
      if (gsym.isType && gsym.hasFlag(gf.COVARIANT)) flags |= mf.COVARIANT
      // TODO: mf.INLINE
      flags
    }

    private def flags: Long = {
      definitionFlags | accessQualifierFlags | otherFlags
    }

    private def name: String = {
      gsym.decodedName.toString
    }

    private def info: String = {
      if (gsym.isClass || gsym.isModule) ""
      else gsym.info.toString.stripPrefix("=> ")
    }

    def toDenotation: s.Denotation = {
      s.Denotation(flags, name, info)
    }
  }
}
